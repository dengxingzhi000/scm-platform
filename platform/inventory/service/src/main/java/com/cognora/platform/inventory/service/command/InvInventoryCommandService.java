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
import com.cognora.platform.inventory.service.cost.CostContext;
import com.cognora.platform.inventory.service.cost.CostMovement;
import com.cognora.platform.inventory.service.cost.InventoryCostLogService;
import com.cognora.platform.inventory.service.cost.MovingAverageCostCalculator;
import com.cognora.platform.inventory.service.cost.MovingAverageCostService;
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
    private final MovingAverageCostService costService;
    private final InventoryCostLogService costLogService;

    public InvInventoryCommandService(InvInventoryMapper inventoryMapper, OutboxService outboxService,
                                      InventoryFlowLogService flowLogService,
                                      MovingAverageCostService costService,
                                      InventoryCostLogService costLogService) {
        this.inventoryMapper = inventoryMapper;
        this.outboxService = outboxService;
        this.flowLogService = flowLogService;
        this.costService = costService;
        this.costLogService = costLogService;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        int adjustType = request.getAdjustType() == null ? 0 : request.getAdjustType();

        Inventory inventory = findInventory(request.getSkuId(), request.getWarehouseId());
        if (inventory == null) {
            if (quantity < 0) {
                throw new IllegalArgumentException(
                        "库存不足，无法扣减。当前库存: 0, 扣减数量: " + Math.abs(quantity));
            }
            inventory = initInventoryEntity(request.getSkuId(), request.getWarehouseId(), 0, BigDecimal.ZERO);
            inventoryMapper.insert(inventory);
        }

        if (quantity == 0) {
            return convertToResponse(inventory);
        }

        Inventory fresh;
        CostMovement movement;
        if (quantity > 0) {
            // 入库：数量与均价在一条版本守卫的 UPDATE 内原子变更
            movement = costService.applyInbound(inventory.getId(), quantity,
                    request.getUnitPrice(), adjustType, request.getOperatorId());
            fresh = reload(inventory.getId());
        } else {
            int updated = inventoryMapper.applyAdjustment(
                    inventory.getId(), quantity, adjustType, request.getOperatorId());
            if (updated == 0) {
                long deducted = -(long) quantity;
                throw new IllegalArgumentException(
                        "库存不足，无法扣减。当前库存: " + inventory.getAvailableStock()
                                + ", 扣减数量: " + deducted);
            }
            fresh = reload(inventory.getId());

            // 出库按当时均价计价，不改变均价；变更前结存由变更后数量反推
            int beforeBalance = Math.subtractExact(nvl(fresh.getTotalStock()), quantity);
            movement = MovingAverageCostService.outboundMovement(
                    fresh.getAverageCost(), beforeBalance, Math.negateExact(quantity));
        }

        String businessType = businessTypeOf(adjustType, quantity);
        String idempotencyKey = adjustmentIdempotencyKey(request, quantity);
        recordAdjustmentFlow(tenantId, fresh, request, quantity, businessType, idempotencyKey);

        costLogService.record(fresh, movement, CostContext.builder()
                .businessType(businessType)
                .businessKey(request.getReferenceNo())
                .referenceType(adjustType == 7 ? "TRANSFER" : "ADJUST")
                .referenceId(fresh.getId())
                .referenceNo(request.getReferenceNo())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .remark(request.getRemark())
                .idempotencyKey(idempotencyKey)
                .build(), tenantId.toString());

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

    /**
     * 单据化调拨：转出按转出仓均价出库（不改转出均价），转入以该单价入库并重算转入仓均价。
     * 两侧数量、成本流水同事务；失败整体回滚。
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean transferInventory(InventoryTransferRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new IllegalArgumentException("源仓库和目标仓库不能相同");
        }
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("调拨数量必须大于0");
        }
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        Inventory from = findInventory(request.getSkuId(), request.getFromWarehouseId());
        if (from == null) {
            throw new IllegalArgumentException("源仓库库存不存在，无法调拨");
        }
        Inventory to = findInventory(request.getSkuId(), request.getToWarehouseId());
        if (to == null) {
            to = initInventoryEntity(request.getSkuId(), request.getToWarehouseId(), 0, BigDecimal.ZERO);
            inventoryMapper.insert(to);
        }

        // 1) 转出：按当时均价出库
        int updated = inventoryMapper.applyAdjustment(
                from.getId(), -quantity, 7, request.getOperatorId());
        if (updated == 0) {
            throw new IllegalArgumentException(
                    "库存不足，无法调拨。当前库存: " + from.getAvailableStock()
                            + ", 调拨数量: " + quantity);
        }
        Inventory freshFrom = reload(from.getId());
        BigDecimal transferUnitPrice = MovingAverageCostCalculator.priceForStorage(
                freshFrom.getAverageCost());
        int outBeforeBalance = Math.addExact(nvl(freshFrom.getTotalStock()), quantity);

        String outBusinessType = "TRANSFER_OUT";
        String inBusinessType = "TRANSFER_IN";
        String adjacentInKey = transferSideKey(request.getTransferNo(), "IN");
        String adjacentOutKey = transferSideKey(request.getTransferNo(), "OUT");
        String outIdempotency = transferIdempotencyKey(request.getTransferNo(), -1);
        String inIdempotency = transferIdempotencyKey(request.getTransferNo(), 1);

        recordTransferFlow(tenantId, freshFrom, request, -quantity, outBusinessType, outIdempotency);
        costLogService.record(freshFrom,
                MovingAverageCostService.outboundMovement(
                        freshFrom.getAverageCost(), outBeforeBalance, quantity),
                transferCostContext(request, outBusinessType, outIdempotency, adjacentInKey, freshFrom),
                tenantId.toString());

        // 2) 转入：以转出仓均价作为入库单价，重算转入仓均价
        CostMovement inMovement = costService.applyInbound(
                to.getId(), quantity, transferUnitPrice, 7, request.getOperatorId());
        Inventory freshTo = reload(to.getId());

        recordTransferFlow(tenantId, freshTo, request, quantity, inBusinessType, inIdempotency);
        costLogService.record(freshTo, inMovement,
                transferCostContext(request, inBusinessType, inIdempotency, adjacentOutKey, freshTo),
                tenantId.toString());

        saveTransferOutbox(tenantId, freshFrom, -quantity);
        saveTransferOutbox(tenantId, freshTo, quantity);
        return true;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponse initInventory(String skuId, String warehouseId, Integer initialStock,
                                           BigDecimal unitPrice) {
        Inventory existing = findInventory(skuId, warehouseId);
        if (existing != null) {
            return convertToResponse(existing);
        }
        int stock = initialStock != null ? initialStock : 0;
        Inventory inventory = initInventoryEntity(skuId, warehouseId, stock, unitPrice);
        inventoryMapper.insert(inventory);

        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        String idempotencyKey = "INIT:" + skuId + ":" + warehouseId;
        boolean logged = recordInitFlow(tenantId, inventory, stock, idempotencyKey);
        if (logged && stock > 0) {
            BigDecimal price = MovingAverageCostCalculator.priceForStorage(unitPrice);
            CostMovement movement = CostMovement.builder()
                    .direction(CostMovement.DIRECTION_IN)
                    .quantity(stock)
                    .unitPrice(price)
                    .amount(MovingAverageCostCalculator.amount(price, stock))
                    .beforeAverageCost(BigDecimal.ZERO)
                    .afterAverageCost(price)
                    .beforeBalanceQty(0)
                    .afterBalanceQty(stock)
                    .build();
            costLogService.record(inventory, movement, CostContext.builder()
                    .businessType("INIT")
                    .referenceType("INVENTORY")
                    .referenceId(inventory.getId())
                    .remark("初始化库存")
                    .idempotencyKey(idempotencyKey)
                    .build(), tenantId);
        }
        return convertToResponse(inventory);
    }

    private Inventory findInventory(String skuId, String warehouseId) {
        return inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId)
                        .eq(Inventory::getWarehouseId, warehouseId)
                        .eq(Inventory::getDeleted, false)
        );
    }

    private Inventory reload(String inventoryId) {
        Inventory fresh = inventoryMapper.selectById(inventoryId);
        if (fresh == null) {
            // 条件更新已成功，读不到记录属于数据异常，不再用内存值推算（避免溢出与错报）
            throw new IllegalStateException("库存变更后未能读取台账记录: id=" + inventoryId);
        }
        return fresh;
    }

    private void recordAdjustmentFlow(UUID tenantId, Inventory fresh,
                                      InventoryAdjustRequest request, int quantity,
                                      String businessType, String idempotencyKey) {
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
                .businessType(businessType)
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
                .idempotencyKey(idempotencyKey)
                .build());
    }

    private boolean recordInitFlow(String tenantId, Inventory inventory, int initialStock,
                                   String idempotencyKey) {
        return flowLogService.record(FlowLogEntry.builder()
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
                .idempotencyKey(idempotencyKey)
                .build());
    }

    private void recordTransferFlow(UUID tenantId, Inventory fresh,
                                    InventoryTransferRequest request, int changeQuantity,
                                    String businessType, String idempotencyKey) {
        int total = nvl(fresh.getTotalStock());
        int available = nvl(fresh.getAvailableStock());
        int locked = nvl(fresh.getLockedStock());
        int damaged = nvl(fresh.getDamagedStock());
        int beforeTotal = Math.subtractExact(total, changeQuantity);
        int beforeAvailable = Math.subtractExact(available, changeQuantity);
        flowLogService.record(FlowLogEntry.builder()
                .tenantId(tenantId.toString())
                .skuId(fresh.getSkuId())
                .warehouseId(fresh.getWarehouseId())
                .operationType(7)
                .businessType(businessType)
                .changeQuantity(changeQuantity)
                .beforeTotal(beforeTotal)
                .beforeAvailable(beforeAvailable)
                .beforeLocked(locked)
                .beforeDamaged(damaged)
                .afterTotal(total)
                .afterAvailable(available)
                .afterLocked(locked)
                .afterDamaged(damaged)
                .reason(request.getRemark())
                .businessKey(request.getTransferNo())
                .referenceType("TRANSFER")
                .referenceId(fresh.getId())
                .referenceNo(request.getTransferNo())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .idempotencyKey(idempotencyKey)
                .build());
    }

    private static CostContext transferCostContext(InventoryTransferRequest request,
                                                   String businessType, String idempotencyKey,
                                                   String counterpartKey, Inventory inventory) {
        return CostContext.builder()
                .businessType(businessType)
                .businessKey(request.getTransferNo())
                .referenceType("TRANSFER")
                .referenceId(inventory.getId())
                .referenceNo(request.getTransferNo())
                .counterpartKey(counterpartKey)
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .remark(request.getRemark())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private void saveTransferOutbox(UUID tenantId, Inventory fresh, int changeQuantity) {
        outboxService.save(
                "INVENTORY_ADJUSTED",
                "Inventory",
                fresh.getId(),
                new InventoryAdjustedEvent(
                        tenantId,
                        fresh.getSkuId(),
                        fresh.getWarehouseId(),
                        changeQuantity,
                        fresh.getAvailableStock()),
                tenantId);
    }

    private static String transferSideKey(String transferNo, String side) {
        return transferNo == null || transferNo.trim().isEmpty()
                ? null : "TRANSFER_" + side + ":" + transferNo;
    }

    private static String transferIdempotencyKey(String transferNo, int direction) {
        if (transferNo == null || transferNo.trim().isEmpty()) {
            return null;
        }
        return "ADJUST:" + transferNo + ":7:" + (direction < 0 ? "OUT" : "IN");
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

    private Inventory initInventoryEntity(String skuId, String warehouseId, Integer initialStock,
                                          BigDecimal unitPrice) {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID().toString());
        inventory.setSkuId(skuId);
        inventory.setWarehouseId(warehouseId);
        int stock = initialStock == null ? 0 : initialStock;
        inventory.setTotalStock(stock);
        inventory.setAvailableStock(stock);
        inventory.setLockedStock(0);
        inventory.setDamagedStock(0);
        inventory.setSafetyStock(10);
        inventory.setAverageCost(stock > 0
                ? MovingAverageCostCalculator.priceForStorage(unitPrice)
                : BigDecimal.ZERO);
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
