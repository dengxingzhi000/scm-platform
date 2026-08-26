package com.scmcloud.inventory.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.inventory.dto.InventoryAdjustRequest;
import com.scmcloud.inventory.dto.InventoryResponse;
import com.scmcloud.inventory.dto.InventoryTransferRequest;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvInventoryCommandService {
    private final InvInventoryMapper inventoryMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, request.getSkuId())
                        .eq(Inventory::getWarehouseId, request.getWarehouseId())
                        .eq(Inventory::getDeleted, false)
        );
        if (inventory == null) {
            inventory = initInventoryEntity(request.getSkuId(), request.getWarehouseId(), 0);
        }
        int beforeStock = inventory.getAvailableStock();
        int afterStock = beforeStock + request.getQuantity();
        if (afterStock < 0) {
            throw new IllegalArgumentException("库存不足，无法扣减。当前库存: " + beforeStock + ", 扣减数量: " + Math.abs(request.getQuantity()));
        }
        inventory.setAvailableStock(afterStock);
        inventory.setTotalStock(inventory.getTotalStock() + request.getQuantity());
        inventory.setUpdateTime(LocalDateTime.now());
        inventory.setUpdateBy(request.getOperatorId());
        if (request.getAdjustType() == 1) {
            inventory.setLastInboundAt(LocalDateTime.now());
        } else if (request.getAdjustType() == 2) {
            inventory.setLastOutboundAt(LocalDateTime.now());
        }
        if (inventory.getId() == null) {
            inventoryMapper.insert(inventory);
        } else {
            inventoryMapper.updateById(inventory);
        }
        return convertToResponse(inventory);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean transferInventory(InventoryTransferRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new IllegalArgumentException("源仓库和目标仓库不能相同");
        }
        InventoryAdjustRequest deductRequest = new InventoryAdjustRequest();
        deductRequest.setSkuId(request.getSkuId());
        deductRequest.setWarehouseId(request.getFromWarehouseId());
        deductRequest.setQuantity(-request.getQuantity());
        deductRequest.setAdjustType(7);
        deductRequest.setReferenceNo(request.getTransferNo());
        deductRequest.setOperatorId(request.getOperatorId());
        deductRequest.setOperatorName(request.getOperatorName());
        deductRequest.setRemark("调拨出库: " + request.getRemark());
        adjustInventory(deductRequest);
        InventoryAdjustRequest addRequest = new InventoryAdjustRequest();
        addRequest.setSkuId(request.getSkuId());
        addRequest.setWarehouseId(request.getToWarehouseId());
        addRequest.setQuantity(request.getQuantity());
        addRequest.setAdjustType(7);
        addRequest.setReferenceNo(request.getTransferNo());
        addRequest.setOperatorId(request.getOperatorId());
        addRequest.setOperatorName(request.getOperatorName());
        addRequest.setRemark("调拨入库: " + request.getRemark());
        adjustInventory(addRequest);
        return true;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponse initInventory(String skuId, String warehouseId, Integer initialStock) {
        Inventory existing = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId)
                        .eq(Inventory::getWarehouseId, warehouseId)
                        .eq(Inventory::getDeleted, false)
        );
        if (existing != null) {
            return convertToResponse(existing);
        }
        Inventory inventory = initInventoryEntity(skuId, warehouseId, initialStock != null ? initialStock : 0);
        inventoryMapper.insert(inventory);
        return convertToResponse(inventory);
    }

    private Inventory initInventoryEntity(String skuId, String warehouseId, Integer initialStock) {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID().toString());
        inventory.setSkuId(skuId);
        inventory.setWarehouseId(warehouseId);
        inventory.setTotalStock(initialStock);
        inventory.setAvailableStock(initialStock);
        inventory.setLockedStock(0);
        inventory.setDamagedStock(0);
        inventory.setSafetyStock(10);
        inventory.setAverageCost(BigDecimal.ZERO);
        inventory.setVersion(0);
        inventory.setDeleted(false);
        inventory.setCreateTime(LocalDateTime.now());
        inventory.setUpdateTime(LocalDateTime.now());
        return inventory;
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
