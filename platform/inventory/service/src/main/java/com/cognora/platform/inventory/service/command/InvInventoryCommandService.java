package com.cognora.platform.inventory.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.platform.common.integration.outbox.OutboxService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryAdjustRequest;
import com.cognora.platform.inventory.dto.InventoryResponse;
import com.cognora.platform.inventory.dto.InventoryTransferRequest;
import com.cognora.platform.inventory.event.InventoryAdjustedEvent;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.flow.FlowLogEntry;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class InvInventoryCommandService {
    private final InvInventoryMapper inventoryMapper;
    private final OutboxService outboxService;
    private final InventoryFlowLogService flowLogService;

    public InvInventoryCommandService(InvInventoryMapper inventoryMapper, OutboxService outboxService,
                                      InventoryFlowLogService flowLogService) {
        this.inventoryMapper = inventoryMapper;
        this.outboxService = outboxService;
        this.flowLogService = flowLogService;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();

        Inventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, request.getSkuId())
                        .eq(Inventory::getWarehouseId, request.getWarehouseId())
                        .eq(Inventory::getDeleted, false)
        );
        if (inventory == null) {
            if (quantity < 0) {
                throw new IllegalArgumentException(
                        "库存不足，无法扣减。当前库存: 0, 扣减数量: " + Math.abs(quantity));
            }
            inventory = initInventoryEntity(request.getSkuId(), request.getWarehouseId(), 0);
            inventoryMapper.insert(inventory);
        }

        // 条件更新：where available_stock + quantity >= 0，并发下不会丢失更新、不会扣成负数
        int updated = inventoryMapper.applyAdjustment(
                inventory.getId(),
                quantity,
                request.getAdjustType() == null ? 0 : request.getAdjustType(),
                request.getOperatorId());
        if (updated == 0) {
            long deducted = quantity < 0 ? -(long) quantity : (long) quantity;
            throw new IllegalArgumentException(
                    "库存不足，无法扣减。当前库存: " + inventory.getAvailableStock()
                            + ", 扣减数量: " + deducted);
        }

        Inventory fresh = inventoryMapper.selectById(inventory.getId());
        if (fresh == null) {
            // 条件更新已成功，读不到记录属于数据异常，不再用内存值推算（避免溢出与错报）
            throw new IllegalStateException("库存调整后未能读取台账记录: id=" + inventory.getId());
        }

        recordAdjustmentFlow(tenantId, fresh, request, quantity);

        outboxService.save(
                "INVENTORY_ADJUSTED",
                "Inventory",
                fresh.getId(),
                new InventoryAdjustedEvent(
                        tenantId,
                        fresh.getSkuId(),
                        fresh.getWarehouseId(),
                        quantity,
                        fresh.getAvailableStock()),
                tenantId);
        return convertToResponse(fresh);
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
        recordInitFlow(inventory, initialStock != null ? initialStock : 0);
        return convertToResponse(inventory);
    }

    private void recordAdjustmentFlow(UUID tenantId, Inventory fresh,
                                      InventoryAdjustRequest request, int quantity) {
        int total = nvl(fresh.getTotalStock());
        int available = nvl(fresh.getAvailableStock());
        int locked = nvl(fresh.getLockedStock());
        int damaged = nvl(fresh.getDamagedStock());
        int adjustType = request.getAdjustType() == null ? 0 : request.getAdjustType();
        flowLogService.record(FlowLogEntry.builder()
                .tenantId(tenantId.toString())
                .skuId(fresh.getSkuId())
                .warehouseId(fresh.getWarehouseId())
                .operationType(adjustType)
                .businessType(businessTypeOf(adjustType, quantity))
                .changeQuantity(quantity)
                .beforeTotal(total - quantity)
                .beforeAvailable(available - quantity)
                .beforeLocked(locked)
                .beforeDamaged(damaged)
                .afterTotal(total)
                .afterAvailable(available)
                .afterLocked(locked)
                .afterDamaged(damaged)
                .reason(request.getRemark())
                .businessKey(request.getReferenceNo())
                .referenceType(adjustType == 7 ? "TRANSFER" : "ADJUST")
                .referenceId(fresh.getId())
                .referenceNo(request.getReferenceNo())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .idempotencyKey(adjustmentIdempotencyKey(request, quantity))
                .build());
    }

    private void recordInitFlow(Inventory inventory, int initialStock) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        flowLogService.record(FlowLogEntry.builder()
                .tenantId(tenantId)
                .skuId(inventory.getSkuId())
                .warehouseId(inventory.getWarehouseId())
                .operationType(initialStock < 0 ? 2 : 1)
                .businessType("INIT")
                .changeQuantity(initialStock)
                .beforeTotal(0)
                .beforeAvailable(0)
                .beforeLocked(0)
                .beforeDamaged(0)
                .afterTotal(initialStock)
                .afterAvailable(initialStock)
                .afterLocked(0)
                .afterDamaged(0)
                .reason("初始化库存")
                .referenceType("INVENTORY")
                .referenceId(inventory.getId())
                .idempotencyKey("INIT:" + inventory.getSkuId() + ":" + inventory.getWarehouseId())
                .build());
    }

    private static String adjustmentIdempotencyKey(InventoryAdjustRequest request, int quantity) {
        if (request.getReferenceNo() == null || request.getReferenceNo().trim().isEmpty()) {
            return null;
        }
        String direction = quantity < 0 ? "OUT" : "IN";
        return "ADJUST:" + request.getReferenceNo() + ":" + request.getAdjustType() + ":" + direction;
    }

    private static String businessTypeOf(int adjustType, int quantity) {
        switch (adjustType) {
            case 1:
                return "INBOUND";
            case 2:
                return "OUTBOUND";
            case 7:
                return quantity < 0 ? "TRANSFER_OUT" : "TRANSFER_IN";
            default:
                return "ADJUST";
        }
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
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
