package com.cognora.platform.inventory.service.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cognora.platform.inventory.api.InventoryDubboService;
import com.cognora.platform.inventory.api.exception.InsufficientStockException;
import com.cognora.platform.inventory.api.request.BatchDeductStockRequest;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.RedisInventoryService;
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

        log.info("Dubbo批量扣减库存成功: itemCount={}", items.size());
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
