package com.cognora.platform.inventory.service.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cognora.platform.inventory.api.InventoryDubboService;
import com.cognora.platform.inventory.api.exception.InsufficientStockException;
import com.cognora.platform.inventory.api.request.BatchDeductStockRequest;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.RedisInventoryService;
import com.cognora.platform.inventory.service.flow.FlowLogEntry;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存Dubbo服务实现
 *
 * <p>提供库存查询、扣减、释放等RPC接口，供其他微服务调用
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class InventoryDubboServiceImpl implements InventoryDubboService {
    private static final String DEFAULT_WAREHOUSE = "DEFAULT";

    private final RedisInventoryService redisInventoryService;
    private final InvInventoryMapper inventoryMapper;
    private final InventoryFlowLogService flowLogService;

    @Override
    public void deductStock(Long skuId, Integer quantity, String requestId) {
        if (skuId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("参数非法: skuId和quantity不能为空，quantity必须大于0");
        }
        validateRequestId(requestId);

        log.info("Dubbo扣减库存: skuId={}, quantity={}, requestId={}", skuId, quantity, requestId);

        // 同一 requestId 只允许扣减一次；重复请求直接返回首次结果
        if (!redisInventoryService.acquireDeductRequest(requestId)) {
            log.info("Dubbo扣减库存重复请求，按幂等返回: skuId={}", skuId);
            return;
        }

        try {
            boolean success = redisInventoryService.deductStock(
                    skuId.toString(), DEFAULT_WAREHOUSE, quantity);
            if (!success) {
                throw new InsufficientStockException(
                        String.format("库存不足: skuId=%d, 需要数量%d", skuId, quantity));
            }
        } catch (RuntimeException e) {
            // 扣减失败释放幂等键，允许同一 requestId 重试
            redisInventoryService.releaseDeductRequest(requestId);
            throw e;
        }

        // Redis 与 DB 不在同一事务：流水写失败时补偿本次扣减，再抛出，避免“扣了没账”。
        try {
            recordDeductFlow(skuId, quantity, requestId);
        } catch (RuntimeException flowFailure) {
            log.error("库存流水写入失败，回滚 Redis 扣减: skuId={}, requestId={}", skuId, requestId);
            compensate(redisInventoryService, skuId.toString(), DEFAULT_WAREHOUSE, quantity);
            throw flowFailure;
        }

        log.info("Dubbo扣减库存成功: skuId={}, quantity={}", skuId, quantity);
    }

    @Override
    public void batchDeductStock(BatchDeductStockRequest deductRequest) {
        if (deductRequest == null || deductRequest.getItems() == null || deductRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("批量扣减请求不能为空");
        }
        validateRequestId(deductRequest.getRequestId());

        List<RedisInventoryService.StockDeductItem> items = new java.util.ArrayList<>();
        for (BatchDeductStockRequest.StockItem item : deductRequest.getItems()) {
            if (item == null || item.getSkuId() == null
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("批量扣减条目非法: skuId 与 quantity 必填且数量大于0");
            }
            items.add(new RedisInventoryService.StockDeductItem(
                    item.getSkuId().toString(), DEFAULT_WAREHOUSE, item.getQuantity()));
        }

        log.info("Dubbo批量扣减库存: itemCount={}", items.size());

        if (!redisInventoryService.acquireDeductRequest(deductRequest.getRequestId())) {
            log.info("Dubbo批量扣减重复请求，按幂等返回: itemCount={}", items.size());
            return;
        }

        try {
            // 整批在单个 Lua 脚本内先校验后扣减，失败不会留下半成品
            if (!redisInventoryService.batchDeductStock(items)) {
                throw new InsufficientStockException("批量扣减库存不足");
            }
        } catch (RuntimeException e) {
            redisInventoryService.releaseDeductRequest(deductRequest.getRequestId());
            throw e;
        }

        try {
            recordBatchDeductFlow(deductRequest);
        } catch (RuntimeException flowFailure) {
            log.error("批量流水写入失败，回滚 Redis 扣减: requestId={}", deductRequest.getRequestId());
            for (BatchDeductStockRequest.StockItem item : deductRequest.getItems()) {
                compensate(redisInventoryService, item.getSkuId().toString(),
                        DEFAULT_WAREHOUSE, item.getQuantity());
            }
            throw flowFailure;
        }

        log.info("Dubbo批量扣减库存成功: itemCount={}", items.size());
    }

    /**
     * 写单次 Dubbo 扣减流水。
     *
     * <p>Redis 是此路径的唯一事实来源，因此 before/after 只填 available（来自 Redis）；
     * total/locked/damaged 不从台账臆造，保持为空并标记 {@code ledger_source=REDIS}。
     */
    private void recordDeductFlow(Long skuId, int quantity, String requestId) {
        Integer afterStock = redisInventoryService.getStock(skuId.toString(), DEFAULT_WAREHOUSE);
        if (afterStock == null) {
            log.warn("Redis 扣减后读不到库存，跳过流水: skuId={}, requestId={}", skuId, requestId);
            return;
        }
        flowLogService.record(deductEntry(skuId, quantity, afterStock,
                "DUBBO_DEDUCT:" + requestId + ":" + skuId, requestId));
    }

    private void recordBatchDeductFlow(BatchDeductStockRequest deductRequest) {
        int index = 0;
        for (BatchDeductStockRequest.StockItem item : deductRequest.getItems()) {
            Integer afterStock =
                    redisInventoryService.getStock(item.getSkuId().toString(), DEFAULT_WAREHOUSE);
            if (afterStock == null) {
                log.warn("批量扣减后读不到库存，跳过该条流水: skuId={}, requestId={}",
                        item.getSkuId(), deductRequest.getRequestId());
                index++;
                continue;
            }
            flowLogService.record(deductEntry(item.getSkuId(), item.getQuantity(), afterStock,
                    "DUBBO_BATCH:" + deductRequest.getRequestId() + ":" + index + ":" + item.getSkuId(),
                    deductRequest.getRequestId()));
            index++;
        }
    }

    private static FlowLogEntry deductEntry(Long skuId, int quantity, int afterStock,
                                            String idempotencyKey, String requestId) {
        return FlowLogEntry.builder()
                .tenantId(currentTenant())
                .legacySkuId(skuId)
                .warehouseCode(DEFAULT_WAREHOUSE)
                .operationType(2)
                .businessType("DUBBO_DEDUCT")
                .changeQuantity(-quantity)
                .beforeAvailable(afterStock + quantity)
                .afterAvailable(afterStock)
                .requestId(requestId)
                .businessKey(requestId)
                .referenceType("DUBBO")
                .reason("Dubbo 扣减库存")
                .ledgerSource("REDIS")
                .extraData("{\"source\":\"REDIS\"}")
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private static String currentTenant() {
        return TenantContextHolder.getTenantId() == null
                ? null : TenantContextHolder.getTenantId().toString();
    }

    private static void compensate(RedisInventoryService redis, String skuId, String warehouse,
                                   int quantity) {
        try {
            redis.addStock(skuId, warehouse, quantity);
        } catch (RuntimeException compensateFailure) {
            log.error("Redis 扣减补偿失败，需人工核对: skuId={}, warehouse={}, quantity={}",
                    skuId, warehouse, quantity, compensateFailure);
        }
    }

    private static void validateRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("requestId 不能为空，扣减必须幂等");
        }
    }

    @Override
    public void releaseStock(Long skuId, Integer quantity, String requestId) {
        if (skuId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("参数非法: skuId和quantity不能为空，quantity必须大于0");
        }

        log.info("Dubbo释放库存: skuId={}, quantity={}, requestId={}", skuId, quantity, requestId);

        String skuIdStr = skuId.toString();

        // 使用Redis增加库存
        Integer afterStock = redisInventoryService.addStock(skuIdStr, DEFAULT_WAREHOUSE, quantity);
        if (afterStock == null) {
            throw new RuntimeException("释放库存失败: skuId=" + skuId);
        }

        log.info("Dubbo释放库存成功: skuId={}, quantity={}, afterStock={}, requestId={}",
                skuId, quantity, afterStock, requestId);
    }

    @Override
    public Integer queryAvailableStock(Long skuId) {
        if (skuId == null) {
            throw new IllegalArgumentException("skuId不能为空");
        }

        log.debug("Dubbo查询可用库存: skuId={}", skuId);

        String skuIdStr = skuId.toString();

        // 先尝试从Redis获取
        Integer stock = redisInventoryService.getStock(skuIdStr, DEFAULT_WAREHOUSE);
        if (stock != null) {
            return stock;
        }

        // Redis未命中，从数据库查询所有仓库的可用库存总和
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getSkuId, skuIdStr)
                .eq(Inventory::getDeleted, false)
                .select(Inventory::getAvailableStock);

        List<Inventory> inventories = inventoryMapper.selectList(wrapper);
        int totalAvailable = inventories.stream()
                .mapToInt(Inventory::getAvailableStock)
                .sum();

        log.debug("Dubbo查询可用库存结果: skuId={}, availableStock={}", skuId, totalAvailable);
        return totalAvailable;
    }
}
