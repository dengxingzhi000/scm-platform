package com.scmcloud.inventory.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.inventory.dto.InventoryQueryRequest;
import com.scmcloud.inventory.dto.InventoryResponse;
import com.scmcloud.inventory.dto.InventoryStatsResponse;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvInventoryQueryService {
    private final InvInventoryMapper inventoryMapper;

    @Slave
    public InventoryResponse getInventory(String skuId, String warehouseId) {
        LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Inventory::getSkuId, skuId);
        wrapper.eq(Inventory::getWarehouseId, warehouseId);
        wrapper.eq(Inventory::getDeleted, false);
        Inventory inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            return null;
        }
        return convertToResponse(inventory);
    }

    @Slave
    public List<InventoryResponse> batchGetInventory(List<String> skuIds, String warehouseId) {
        if (CollectionUtils.isEmpty(skuIds)) {
            return List.of();
        }
        LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery();
        wrapper.in(Inventory::getSkuId, skuIds);
        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(Inventory::getWarehouseId, warehouseId);
        }
        wrapper.eq(Inventory::getDeleted, false);
        List<Inventory> inventories = inventoryMapper.selectList(wrapper);
        return inventories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Slave
    public Page<InventoryResponse> queryInventory(InventoryQueryRequest request) {
        LambdaQueryWrapper<Inventory> wrapper = buildQueryWrapper(request);
        Page<Inventory> page = inventoryMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
        Page<InventoryResponse> responsePage = new Page<>();
        BeanUtils.copyProperties(page, responsePage, "records");
        responsePage.setRecords(
                page.getRecords().stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList())
        );
        return responsePage;
    }

    @Slave
    public boolean checkStockAvailable(String skuId, String warehouseId, Integer quantity) {
        LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Inventory::getSkuId, skuId);
        wrapper.eq(Inventory::getWarehouseId, warehouseId);
        wrapper.eq(Inventory::getDeleted, false);
        Inventory inventory = inventoryMapper.selectOne(wrapper);
        if (inventory == null) {
            return false;
        }
        return inventory.getAvailableStock() >= quantity;
    }

    @Slave
    public InventoryStatsResponse getInventoryStats() {
        LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Inventory::getDeleted, false);
        List<Inventory> allInventories = inventoryMapper.selectList(wrapper);

        InventoryStatsResponse stats = new InventoryStatsResponse();
        stats.setTotalSkuCount((long) allInventories.stream()
                .map(Inventory::getSkuId)
                .distinct()
                .count());
        stats.setTotalWarehouseCount((long) allInventories.stream()
                .map(Inventory::getWarehouseId)
                .distinct()
                .count());
        stats.setTotalStockQuantity(allInventories.stream()
                .mapToLong(Inventory::getTotalStock)
                .sum());
        stats.setAvailableStockQuantity(allInventories.stream()
                .mapToLong(Inventory::getAvailableStock)
                .sum());
        stats.setLockedStockQuantity(allInventories.stream()
                .mapToLong(Inventory::getLockedStock)
                .sum());
        stats.setDamagedStockQuantity(allInventories.stream()
                .mapToLong(Inventory::getDamagedStock)
                .sum());
        stats.setTotalStockValue(allInventories.stream()
                .map(inv -> inv.getAverageCost().multiply(BigDecimal.valueOf(inv.getTotalStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.setOutOfStockCount(allInventories.stream()
                .filter(inv -> inv.getAvailableStock() == 0)
                .count());
        stats.setLowStockCount(allInventories.stream()
                .filter(inv -> inv.getAvailableStock() > 0 && inv.getAvailableStock() <= inv.getSafetyStock())
                .count());
        stats.setNormalStockCount(allInventories.stream()
                .filter(inv -> inv.getAvailableStock() > inv.getSafetyStock())
                .count());
        return stats;
    }

    private LambdaQueryWrapper<Inventory> buildQueryWrapper(InventoryQueryRequest request) {
        LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery();
        if (!CollectionUtils.isEmpty(request.getSkuIds())) {
            wrapper.in(Inventory::getSkuId, request.getSkuIds());
        }
        if (!CollectionUtils.isEmpty(request.getWarehouseIds())) {
            wrapper.in(Inventory::getWarehouseId, request.getWarehouseIds());
        }
        if (StringUtils.hasText(request.getLocationCode())) {
            wrapper.eq(Inventory::getLocationCode, request.getLocationCode());
        }
        if (request.getMinAvailableStock() != null) {
            wrapper.ge(Inventory::getAvailableStock, request.getMinAvailableStock());
        }
        if (request.getMaxAvailableStock() != null) {
            wrapper.le(Inventory::getAvailableStock, request.getMaxAvailableStock());
        }
        if (Boolean.TRUE.equals(request.getOnlyInStock())) {
            wrapper.gt(Inventory::getAvailableStock, 0);
        }
        if (StringUtils.hasText(request.getStockStatus())) {
            switch (request.getStockStatus()) {
                case "OUT_OF_STOCK" -> wrapper.eq(Inventory::getAvailableStock, 0);
                case "LOW_STOCK" -> wrapper.apply("available_stock > 0 AND available_stock <= safety_stock");
                case "NORMAL" -> wrapper.apply("available_stock > safety_stock");
            }
        }
        wrapper.eq(Inventory::getDeleted, false);
        if (StringUtils.hasText(request.getSortBy())) {
            boolean isAsc = "ASC".equalsIgnoreCase(request.getSortOrder());
            switch (request.getSortBy()) {
                case "available_stock" -> wrapper.orderBy(true, isAsc, Inventory::getAvailableStock);
                case "total_stock" -> wrapper.orderBy(true, isAsc, Inventory::getTotalStock);
                case "update_time" -> wrapper.orderBy(true, isAsc, Inventory::getUpdateTime);
                default -> wrapper.orderByDesc(Inventory::getUpdateTime);
            }
        } else {
            wrapper.orderByDesc(Inventory::getUpdateTime);
        }
        return wrapper;
    }

    private InventoryResponse convertToResponse(Inventory inventory) {
        InventoryResponse response = new InventoryResponse();
        BeanUtils.copyProperties(inventory, response);
        if (inventory.getAvailableStock() == 0) {
            response.setStockStatus("OUT_OF_STOCK");
        } else if (inventory.getAvailableStock() <= inventory.getSafetyStock()) {
            response.setStockStatus("LOW_STOCK");
        } else {
            response.setStockStatus("NORMAL");
        }
        return response;
    }
}
