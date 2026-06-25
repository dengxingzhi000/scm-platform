package com.scmcloud.inventory.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.inventory.dto.InventoryReservationRequest;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.engine.*;
import com.scmcloud.inventory.lock.DistributedLock;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvReservationCommandService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final InvInventoryMapper inventoryMapper;
    private final DistributedLock distributedLock;
    private final InventoryAllocationEngine allocationEngine;

    private static final String RESERVATION_KEY_PREFIX = "inventory:reservation:";
    private static final String RESERVATION_INDEX_PREFIX = "inventory:reservation:index:";
    private static final int DEFAULT_TIMEOUT_SECONDS = 900;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveInventory(InventoryReservationRequest request) {
        String reservationKey = buildReservationKey(request.getBusinessKey());
        if (Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey))) {
            return true;
        }
        String lockKey = "reserve:" + request.getSkuId() + ":" + request.getWarehouseId();
        DistributedLock.LockHandle lock = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        if (lock == null) {
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        try {
            Inventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getSkuId, request.getSkuId())
                            .eq(Inventory::getWarehouseId, request.getWarehouseId())
                            .eq(Inventory::getDeleted, false)
                            .last("FOR UPDATE")
            );
            if (inventory == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            if (inventory.getAvailableStock() < request.getQuantity()) {
                throw new RuntimeException("库存不足: 可用 " + inventory.getAvailableStock() + ", 需要 " + request.getQuantity());
            }
            int updated = inventoryMapper.update(null,
                    new LambdaUpdateWrapper<Inventory>()
                            .setSql("available_stock = available_stock - " + request.getQuantity())
                            .setSql("locked_stock = locked_stock + " + request.getQuantity())
                            .eq(Inventory::getId, inventory.getId())
                            .ge(Inventory::getAvailableStock, request.getQuantity())
            );
            if (updated == 0) {
                throw new RuntimeException("库存锁定失败，请重试");
            }
            Map<String, Object> reservationData = new HashMap<>();
            reservationData.put("skuId", request.getSkuId());
            reservationData.put("warehouseId", request.getWarehouseId());
            reservationData.put("quantity", request.getQuantity());
            reservationData.put("businessKey", request.getBusinessKey());
            reservationData.put("operatorId", request.getOperatorId());
            reservationData.put("operatorName", request.getOperatorName());
            reservationData.put("remark", request.getRemark());
            reservationData.put("createTime", System.currentTimeMillis());
            int timeoutSeconds = request.getTimeoutSeconds() != null ?
                    request.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
            redisTemplate.opsForHash().putAll(reservationKey, reservationData);
            redisTemplate.expire(reservationKey, timeoutSeconds, TimeUnit.SECONDS);
            String indexKey = buildIndexKey(request.getSkuId(), request.getWarehouseId());
            redisTemplate.opsForSet().add(indexKey, request.getBusinessKey());
            redisTemplate.expire(indexKey, timeoutSeconds, TimeUnit.SECONDS);
            return true;
        } finally {
            lock.release();
        }
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReservation(String businessKey) {
        String reservationKey = buildReservationKey(businessKey);
        Map<Object, Object> reservationData = redisTemplate.opsForHash().entries(reservationKey);
        if (reservationData.isEmpty()) {
            return false;
        }
        String skuId = (String) reservationData.get("skuId");
        String warehouseId = (String) reservationData.get("warehouseId");
        Integer quantity = (Integer) reservationData.get("quantity");
        String lockKey = "confirm:" + skuId + ":" + warehouseId;
        DistributedLock.LockHandle lock = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        if (lock == null) {
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        try {
            int updated = inventoryMapper.update(null,
                    new LambdaUpdateWrapper<Inventory>()
                            .setSql("locked_stock = locked_stock - " + quantity)
                            .setSql("total_stock = total_stock - " + quantity)
                            .eq(Inventory::getSkuId, skuId)
                            .eq(Inventory::getWarehouseId, warehouseId)
                            .ge(Inventory::getLockedStock, quantity)
            );
            if (updated == 0) {
                throw new RuntimeException("锁定库存不足");
            }
            redisTemplate.delete(reservationKey);
            String indexKey = buildIndexKey(skuId, warehouseId);
            redisTemplate.opsForSet().remove(indexKey, businessKey);
            return true;
        } finally {
            lock.release();
        }
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseReservation(String businessKey) {
        String reservationKey = buildReservationKey(businessKey);
        Map<Object, Object> reservationData = redisTemplate.opsForHash().entries(reservationKey);
        if (reservationData.isEmpty()) {
            return false;
        }
        String skuId = (String) reservationData.get("skuId");
        String warehouseId = (String) reservationData.get("warehouseId");
        Integer quantity = (Integer) reservationData.get("quantity");
        String lockKey = "release:" + skuId + ":" + warehouseId;
        DistributedLock.LockHandle lock = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        if (lock == null) {
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        try {
            inventoryMapper.update(null,
                    new LambdaUpdateWrapper<Inventory>()
                            .setSql("available_stock = available_stock + " + quantity)
                            .setSql("locked_stock = locked_stock - " + quantity)
                            .eq(Inventory::getSkuId, skuId)
                            .eq(Inventory::getWarehouseId, warehouseId)
                            .ge(Inventory::getLockedStock, quantity)
            );
            redisTemplate.delete(reservationKey);
            String indexKey = buildIndexKey(skuId, warehouseId);
            redisTemplate.opsForSet().remove(indexKey, businessKey);
            return true;
        } finally {
            lock.release();
        }
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int releaseExpiredReservations() {
        log.warn("当前实现依赖 Redis 自动过期机制，无需手动扫描");
        return 0;
    }

    private String buildReservationKey(String businessKey) {
        return RESERVATION_KEY_PREFIX + businessKey;
    }

    private String buildIndexKey(String skuId, String warehouseId) {
        return RESERVATION_INDEX_PREFIX + skuId + ":" + warehouseId;
    }

    public AllocationOutput smartAllocate(AllocationInput input) {
        log.info("执行智能库存分配: orderId={}, itemCount={}", input.getOrderId(), input.getItems().size());

        List<AllocationInput.OrderItem> itemsWithStock = input.getItems().stream()
                .filter(item -> {
                    List<Inventory> inventories = inventoryMapper.selectList(
                            new LambdaQueryWrapper<Inventory>()
                                    .eq(Inventory::getSkuId, item.getSkuId())
                                    .eq(Inventory::getDeleted, false)
                                    .gt(Inventory::getAvailableStock, 0)
                    );
                    return !inventories.isEmpty();
                })
                .collect(Collectors.toList());

        if (itemsWithStock.isEmpty()) {
            AllocationOutput output = new AllocationOutput();
            output.setAllocations(Collections.emptyList());
            output.setSplitCount(0);
            return output;
        }

        input.setItems(itemsWithStock);
        AllocationOutput output = allocationEngine.allocate(input);

        log.info("智能分配完成: orderId={}, allocations={}, splitCount={}",
                input.getOrderId(), output.getAllocations().size(), output.getSplitCount());
        return output;
    }
}
