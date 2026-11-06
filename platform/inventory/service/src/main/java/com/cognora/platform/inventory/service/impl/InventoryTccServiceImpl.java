package com.cognora.platform.inventory.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.api.InventoryTccService;
import com.cognora.platform.inventory.domain.entity.InvTccReservation;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvTccReservationMapper;
import com.cognora.platform.inventory.service.flow.FlowLogEntry;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存 TCC 服务实现
 *
 * <p>实现 Try-Confirm-Cancel 三阶段库存预留：
 * <ul>
 *   <li>Try：可用量转锁定，落 {@code inv_tcc_reservation}，记录精确的 {@code inventory_id}</li>
 *   <li>Confirm：只允许 TRYING→CONFIRMED，锁定库存实扣（locked 与 total 同步减少，
 *       保持 {@code total = available + locked + damaged}）</li>
 *   <li>Cancel：只允许 TRYING→CANCELLED，锁定库存归还可用量；Try 未执行时反向插入
 *       CANCELLED 记录，防止后续 Try 悬挂</li>
 * </ul>
 *
 * <p>幂等与租户：
 * <ul>
 *   <li>幂等键为 {@code (tenant_id, business_key)}，重复 Try/Confirm/Cancel 不重复扣减</li>
 *   <li>所有查询与状态更新都显式带 tenant_id，跨租户访问抛 {@link AccessDeniedException}</li>
 *   <li>{@code businessKey} 为空时抛 {@link IllegalArgumentException}，不做字符串拼接</li>
 * </ul>
 *
 * <p>SKU 兼容：旧接口接收 Long skuId（scm-order 现状），通过
 * {@code inv_inventory.legacy_sku_id} 解析台账行；新接入方使用
 * {@link #reserveInventoryBySku(String, String, Integer, String)} 按 UUID+仓库精确预留。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryTccServiceImpl implements InventoryTccService {
    private final InvInventoryMapper inventoryMapper;
    private final InvTccReservationMapper reservationMapper;
    private final InventoryFlowLogService flowLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveInventory(Long skuId, Integer quantity, String businessKey) {
        if (skuId == null) {
            throw new IllegalArgumentException("skuId 不能为空");
        }
        String tenant = requireTenant();
        requirePositiveQuantity(quantity);
        requireBusinessKey(businessKey);

        InvTccReservation existing = reservationMapper.selectByBusinessKey(tenant, businessKey);
        if (existing != null) {
            return handleExistingReservation(existing, tenant);
        }

        Inventory inventory = resolveLegacyInventory(tenant, skuId);
        return doReserve(tenant, skuId, inventory.getSkuId(), inventory.getWarehouseId(),
                inventory, quantity, businessKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveInventoryBySku(String skuUuid, String warehouseId,
                                         Integer quantity, String businessKey) {
        if (isBlank(skuUuid) || isBlank(warehouseId)) {
            throw new IllegalArgumentException("skuUuid 和 warehouseId 不能为空");
        }
        String tenant = requireTenant();
        requirePositiveQuantity(quantity);
        requireBusinessKey(businessKey);

        InvTccReservation existing = reservationMapper.selectByBusinessKey(tenant, businessKey);
        if (existing != null) {
            return handleExistingReservation(existing, tenant);
        }

        Inventory inventory = inventoryMapper.selectBySkuUuidAndWarehouse(tenant, skuUuid, warehouseId);
        if (inventory == null) {
            log.error("库存记录不存在，无法预留: warehouseMatch=false");
            throw new IllegalArgumentException("商品不存在或该仓库无库存记录");
        }
        return doReserve(tenant, null, skuUuid, warehouseId, inventory, quantity, businessKey);
    }

    private boolean doReserve(String tenant, Long legacySkuId, String skuUuid, String warehouseId,
                              Inventory inventory, Integer quantity, String businessKey) {
        String xid = RootContext.getXID();

        int locked = inventoryMapper.lockStock(inventory.getId(), quantity);
        if (locked == 0) {
            log.error("库存不足，无法预留: available={}, required={}",
                    inventory.getAvailableStock(), quantity);
            throw new IllegalArgumentException(String.format("库存不足: 可用 %d, 需要%d",
                    inventory.getAvailableStock(), quantity));
        }

        InvTccReservation reservation = new InvTccReservation();
        reservation.setTenantId(tenant);
        reservation.setBusinessKey(businessKey);
        reservation.setSkuId(legacySkuId);
        reservation.setSkuUuid(skuUuid);
        reservation.setInventoryId(inventory.getId());
        reservation.setWarehouseId(warehouseId);
        reservation.setQuantity(quantity);
        reservation.setXid(xid);
        reservation.setBranchId(0L);
        reservation.setStatus(InvTccReservation.Status.TRYING);
        reservation.setTryTime(LocalDateTime.now());

        try {
            reservationMapper.insert(reservation);
        } catch (DuplicateKeyException e) {
            // 并发下同一业务键已写入：本次扣减由其事务回滚，按幂等成功返回
            log.warn("并发写入预留记录，按幂等返回: reservationId={}", reservation.getId());
            return true;
        }

        log.debug("TCC Try 成功: reservationId={}, inventoryId={}, quantity={}",
                reservation.getId(), inventory.getId(), quantity);

        recordFlow(tenant, inventory, quantity, "TCC_TRY", 3, -quantity,
                businessKey, "TCC_TRY:" + businessKey, "TCC 预占锁定");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReserve(BusinessActionContext context) {
        String businessKey = requireActionBusinessKey(context);
        String tenant = requireTenant();

        InvTccReservation reservation = reservationMapper.selectByBusinessKey(tenant, businessKey);
        if (reservation == null) {
            log.warn("TCC Confirm 找不到 Try 记录，拒绝执行（防悬挂）");
            return false;
        }
        assertSameTenant(reservation, tenant);

        String status = reservation.getStatus();
        if (InvTccReservation.Status.CONFIRMED.equals(status)) {
            log.debug("TCC Confirm 幂等返回: reservationId={}", reservation.getId());
            return true;
        }
        if (!InvTccReservation.Status.TRYING.equals(status)) {
            log.warn("TCC Confirm 非法状态流转被拒绝: reservationId={}, status={}",
                    reservation.getId(), status);
            return false;
        }

        int moved = reservationMapper.markConfirmed(reservation.getId(), tenant);
        if (moved == 0) {
            InvTccReservation latest = reservationMapper.selectByBusinessKey(tenant, businessKey);
            boolean confirmed = latest != null
                    && InvTccReservation.Status.CONFIRMED.equals(latest.getStatus());
            log.debug("TCC Confirm 并发竞态: reservationId={}, confirmed={}",
                    reservation.getId(), confirmed);
            return confirmed;
        }

        int quantity = persistedQuantity(reservation);
        Inventory ledger = resolveLedgerForRecord(reservation, tenant);
        int updated = inventoryMapper.confirmStock(ledger.getId(), quantity);
        if (updated == 0) {
            throw new IllegalStateException(
                    "锁定库存不足，无法确认预留: reservationId=" + reservation.getId());
        }

        log.debug("TCC Confirm 成功: reservationId={}, quantity={}", reservation.getId(), quantity);

        recordFlow(tenant, ledger, quantity, "TCC_CONFIRM", 5, -quantity,
                businessKey, "TCC_CONFIRM:" + businessKey, "TCC 确认实扣");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelReserve(BusinessActionContext context) {
        String businessKey = requireActionBusinessKey(context);
        String tenant = requireTenant();

        InvTccReservation reservation = reservationMapper.selectByBusinessKey(tenant, businessKey);
        if (reservation == null) {
            // 空回滚：Try 未执行，插入 CANCELLED 记录防止后续 Try 悬挂
            InvTccReservation tombstone = new InvTccReservation();
            tombstone.setTenantId(tenant);
            tombstone.setBusinessKey(businessKey);
            tombstone.setSkuId(optionalLongActionContext(context, "skuId"));
            tombstone.setSkuUuid(optionalStringActionContext(context, "skuUuid"));
            tombstone.setWarehouseId(optionalStringActionContext(context, "warehouseId"));
            tombstone.setQuantity(optionalIntActionContext(context, "quantity"));
            tombstone.setXid(context.getXid());
            tombstone.setBranchId(0L);
            tombstone.setStatus(InvTccReservation.Status.CANCELLED);
            tombstone.setCancelTime(LocalDateTime.now());
            reservationMapper.insert(tombstone);
            log.warn("TCC Cancel 空回滚：Try 未执行，已写入 CANCELLED 记录");
            return true;
        }
        assertSameTenant(reservation, tenant);

        String status = reservation.getStatus();
        if (InvTccReservation.Status.CANCELLED.equals(status)) {
            log.debug("TCC Cancel 幂等返回: reservationId={}", reservation.getId());
            return true;
        }
        if (!InvTccReservation.Status.TRYING.equals(status)) {
            log.warn("TCC Cancel 非法状态流转被拒绝: reservationId={}, status={}",
                    reservation.getId(), status);
            return false;
        }

        int moved = reservationMapper.markCancelled(reservation.getId(), tenant);
        if (moved == 0) {
            InvTccReservation latest = reservationMapper.selectByBusinessKey(tenant, businessKey);
            boolean cancelled = latest != null
                    && InvTccReservation.Status.CANCELLED.equals(latest.getStatus());
            log.debug("TCC Cancel 并发竞态: reservationId={}, cancelled={}",
                    reservation.getId(), cancelled);
            return cancelled;
        }

        int quantity = persistedQuantity(reservation);
        Inventory ledger = resolveLedgerForRecord(reservation, tenant);
        int released = inventoryMapper.releaseStock(ledger.getId(), quantity);
        if (released == 0) {
            throw new IllegalStateException(
                    "锁定库存不足，无法释放预留: reservationId=" + reservation.getId());
        }

        log.debug("TCC Cancel 成功: reservationId={}, quantity={}", reservation.getId(), quantity);

        recordFlow(tenant, ledger, quantity, "TCC_CANCEL", 4, quantity,
                businessKey, "TCC_CANCEL:" + businessKey, "TCC 取消归还可用");
        return true;
    }

    /**
     * 已存在预留记录时的幂等语义：
     * TRYING / CONFIRMED 视为成功；CANCELLED 为终态，拒绝复用业务键（防悬挂）。
     */
    private boolean handleExistingReservation(InvTccReservation existing, String tenant) {
        assertSameTenant(existing, tenant);
        String status = existing.getStatus();
        if (InvTccReservation.Status.TRYING.equals(status)
                || InvTccReservation.Status.CONFIRMED.equals(status)) {
            log.warn("预留记录已存在，幂等返回: reservationId={}, status={}",
                    existing.getId(), status);
            return true;
        }
        log.warn("业务键已处于终态，拒绝复用: reservationId={}, status={}",
                existing.getId(), status);
        return false;
        }

    /**
     * 按变更前台账快照 + 数量推导变更后快照并写库存流水。
     *
     * <p>幂等键按 businessKey 维度，重复 Confirm/Cancel 回调在状态机处已提前返回，
     * 这里再做一层去重兜底。
     */
    private void recordFlow(String tenant, Inventory before, int quantity,
                            String businessType, int operationType, int changeQuantity,
                            String businessKey, String idempotencyKey, String reason) {
        int beforeTotal = nvl(before.getTotalStock());
        int beforeAvailable = nvl(before.getAvailableStock());
        int beforeLocked = nvl(before.getLockedStock());
        int beforeDamaged = nvl(before.getDamagedStock());

        int afterTotal = beforeTotal;
        int afterAvailable = beforeAvailable;
        int afterLocked = beforeLocked;
        switch (businessType) {
            case "TCC_TRY" -> {
                afterAvailable = beforeAvailable - quantity;
                afterLocked = beforeLocked + quantity;
            }
            case "TCC_CONFIRM" -> {
                afterTotal = beforeTotal - quantity;
                afterLocked = beforeLocked - quantity;
            }
            case "TCC_CANCEL" -> {
                afterAvailable = beforeAvailable + quantity;
                afterLocked = beforeLocked - quantity;
            }
            default -> throw new IllegalArgumentException("未知流水业务类型: " + businessType);
        }

        flowLogService.record(FlowLogEntry.builder()
                .tenantId(tenant)
                .skuId(before.getSkuId())
                .warehouseId(before.getWarehouseId())
                .operationType(operationType)
                .businessType(businessType)
                .changeQuantity(changeQuantity)
                .beforeTotal(beforeTotal)
                .beforeAvailable(beforeAvailable)
                .beforeLocked(beforeLocked)
                .beforeDamaged(beforeDamaged)
                .afterTotal(afterTotal)
                .afterAvailable(afterAvailable)
                .afterLocked(afterLocked)
                .afterDamaged(beforeDamaged)
                .reason(reason)
                .businessKey(businessKey)
                .referenceType("TCC")
                .referenceId(before.getId())
                .idempotencyKey(idempotencyKey)
                .build());
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private Inventory resolveLegacyInventory(String tenant, Long skuId) {
        List<Inventory> rows = inventoryMapper.selectByLegacySkuId(tenant, skuId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "商品不存在或 legacy_sku_id 未配置，无法定位台账行；请使用 reserveInventoryBySku");
        }
        if (rows.size() > 1) {
            throw new IllegalStateException(
                    "同一 SKU 命中多个仓库台账行，无法唯一定位；请使用 reserveInventoryBySku");
        }
        return rows.get(0);
    }

    /**
     * 历史记录可能没有 inventory_id：退回按 (skuUuid, warehouseId) 或 legacy_sku_id 解析。
     * 仍然无法定位时抛异常，不静默跳过扣减/归还。
     */
    private Inventory resolveLedgerForRecord(InvTccReservation reservation, String tenant) {
        if (reservation.getInventoryId() != null) {
            Inventory ledger = inventoryMapper.selectById(reservation.getInventoryId());
            if (ledger != null && (ledger.getTenantId() == null
                    || ledger.getTenantId().equals(tenant))) {
                return ledger;
            }
        }
        if (!isBlank(reservation.getSkuUuid()) && !isBlank(reservation.getWarehouseId())) {
            Inventory ledger = inventoryMapper.selectBySkuUuidAndWarehouse(
                    tenant, reservation.getSkuUuid(), reservation.getWarehouseId());
            if (ledger != null) {
                return ledger;
            }
        }
        if (reservation.getSkuId() != null) {
            List<Inventory> rows = inventoryMapper.selectByLegacySkuId(tenant, reservation.getSkuId());
            if (rows.size() == 1) {
                return rows.get(0);
            }
        }
        throw new IllegalStateException(
                "无法定位预留对应的台账行: reservationId=" + reservation.getId());
    }

    private static int persistedQuantity(InvTccReservation reservation) {
        if (reservation.getQuantity() == null || reservation.getQuantity() <= 0) {
            throw new IllegalStateException(
                    "预留记录数量非法: reservationId=" + reservation.getId());
        }
        return reservation.getQuantity();
    }

    private static String requireTenant() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }

    private static void requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("预占数量必须大于0");
        }
    }

    private static void requireBusinessKey(String businessKey) {
        if (isBlank(businessKey)) {
            throw new IllegalArgumentException("businessKey 不能为空");
        }
    }

    private static String requireActionBusinessKey(BusinessActionContext context) {
        Object value = actionContextValue(context, "businessKey");
        if (value == null || isBlank(value.toString())) {
            throw new IllegalArgumentException("TCC 上下文中 businessKey 不能为空");
        }
        return value.toString();
    }

    private static Object actionContextValue(BusinessActionContext context, String name) {
        if (context == null || context.getActionContext() == null) {
            return null;
        }
        return context.getActionContext(name);
    }

    private static Long optionalLongActionContext(BusinessActionContext context, String name) {
        Object value = actionContextValue(context, name);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("TCC action context '" + name + "' is not a number", e);
        }
    }

    private static Integer optionalIntActionContext(BusinessActionContext context, String name) {
        Object value = context.getActionContext(name);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("TCC action context '" + name + "' is not a number", e);
        }
    }

    private static String optionalStringActionContext(BusinessActionContext context, String name) {
        Object value = context.getActionContext(name);
        return value == null ? null : value.toString();
    }

    private static void assertSameTenant(InvTccReservation reservation, String tenant) {
        if (reservation.getTenantId() != null && !reservation.getTenantId().equals(tenant)) {
            log.warn("跨租户访问 TCC 预留记录被拒绝: reservationId={}, ownerTenant={}",
                    reservation.getId(), reservation.getTenantId());
            throw new AccessDeniedException("预留记录不属于当前租户");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
