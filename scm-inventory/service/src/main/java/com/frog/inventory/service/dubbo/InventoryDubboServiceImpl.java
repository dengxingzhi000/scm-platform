package com.frog.inventory.service.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.frog.inventory.api.InventoryDubboService;
import com.frog.inventory.api.exception.InsufficientStockException;
import com.frog.inventory.api.request.BatchDeductStockRequest;
import com.frog.inventory.domain.entity.Inventory;
import com.frog.inventory.mapper.InvInventoryMapper;
import com.frog.inventory.service.RedisInventoryService;
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

        log.info("Dubbo扣减库存: skuId={}, quantity={}, requestId={}", skuId, quantity, requestId);

        String skuIdStr = skuId.toString();

        // 优先使用Redis扣减
        boolean success = redisInventoryService.deductStock(skuIdStr, DEFAULT_WAREHOUSE, quantity);
        if (!success) {
            throw new InsufficientStockException(
                    String.format("库存不足: skuId=%d, 需要数量=%d", skuId, quantity));
        }

        log.info("Dubbo扣减库存成功: skuId={}, quantity={}, requestId={}", skuId, quantity, requestId);
    }

    @Override
    public void batchDeductStock(BatchDeductStockRequest deductRequest) {
        if (deductRequest == null || deductRequest.getItems() == null || deductRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("批量扣减请求不能为空");
        }

        log.info("Dubbo批量扣减库存: itemCount={}, requestId={}",
                deductRequest.getItems().size(), deductRequest.getRequestId());

        for (BatchDeductStockRequest.StockItem item : deductRequest.getItems()) {
            deductStock(item.getSkuId(), item.getQuantity(), deductRequest.getRequestId());
        }

        log.info("Dubbo批量扣减库存成功: requestId={}", deductRequest.getRequestId());
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
