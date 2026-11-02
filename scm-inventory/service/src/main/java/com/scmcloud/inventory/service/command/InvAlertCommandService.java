package com.scmcloud.inventory.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.inventory.domain.entity.InvAlert;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.dto.InvAlertScanResult;
import com.scmcloud.inventory.mapper.InvAlertMapper;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import com.scmcloud.inventory.service.notification.InventoryAlertNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvAlertCommandService {

    /**
     * 库存不足告警
     */
    private static final int ALERT_TYPE_LOW_STOCK = 1;

    /**
     * 库存为 0 告警
     */
    private static final int ALERT_TYPE_OUT_OF_STOCK = 2;

    private final InvAlertMapper alertMapper;
    private final InvInventoryMapper inventoryMapper;
    private final InventoryAlertNotificationPublisher notificationPublisher;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(InvAlert alert) {
        return alertMapper.insert(alert);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(InvAlert alert) {
        return alertMapper.updateById(alert);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return alertMapper.deleteById(id);
    }

    /**
     * 处理告警：标记为已解决。
     *
     * @param id         告警 ID
     * @param resolvedBy 处理人，缺省记为 SYSTEM
     * @return 更新行数；告警不存在时抛出 IllegalArgumentException，重复处理返回 0
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int resolve(String id, String resolvedBy) {
        InvAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            throw new IllegalArgumentException("告警不存在: " + id);
        }
        if (Boolean.TRUE.equals(alert.getIsResolved())) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        alert.setIsResolved(true);
        alert.setResolvedAt(now);
        alert.setResolvedBy(StringUtils.hasText(resolvedBy) ? resolvedBy : "SYSTEM");
        alert.setUpdateTime(now);
        return alertMapper.updateById(alert);
    }

    /**
     * 扫描全部库存，按可用库存与安全库存生成/刷新告警，并在库存恢复后自动解除告警。
     *
     * <p>由定时任务与手动触发接口共用；定时任务没有租户上下文时扫描全部租户，
     * HTTP 触发时租户拦截器只会返回当前租户的库存。</p>
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public InvAlertScanResult scanAndGenerate() {
        List<Inventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getDeleted, false));
        List<InvAlert> unresolved = alertMapper.selectList(
                new LambdaQueryWrapper<InvAlert>().eq(InvAlert::getIsResolved, false));

        Map<String, InvAlert> openAlerts = new HashMap<>();
        for (InvAlert alert : unresolved) {
            openAlerts.put(alertKey(alert.getTenantId(), alert.getSkuId(), alert.getWarehouseId(), alert.getAlertType()), alert);
        }

        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int updated = 0;
        List<InvAlert> createdAlerts = new ArrayList<>();

        for (Inventory inventory : inventories) {
            Integer alertType = detectAlertType(inventory);
            if (alertType == null) {
                continue;
            }
            String key = alertKey(inventory.getTenantId(), inventory.getSkuId(), inventory.getWarehouseId(), alertType);
            InvAlert existing = openAlerts.remove(key);
            if (existing != null) {
                existing.setCurrentStock(inventory.getAvailableStock());
                existing.setSafetyStock(inventory.getSafetyStock());
                existing.setThresholdValue(thresholdOf(alertType, inventory));
                existing.setUpdateTime(now);
                alertMapper.updateById(existing);
                updated++;
            } else {
                InvAlert alert = new InvAlert();
                alert.setId(UUID.randomUUID().toString());
                alert.setTenantId(inventory.getTenantId());
                alert.setSkuId(inventory.getSkuId());
                alert.setWarehouseId(inventory.getWarehouseId());
                alert.setAlertType(alertType);
                alert.setCurrentStock(inventory.getAvailableStock());
                alert.setSafetyStock(inventory.getSafetyStock());
                alert.setThresholdValue(thresholdOf(alertType, inventory));
                alert.setIsResolved(false);
                alert.setNotified(false);
                alert.setCreateTime(now);
                alert.setUpdateTime(now);
                alertMapper.insert(alert);
                createdAlerts.add(alert);
                created++;
            }
        }

        // 库存已恢复：只自动解除库存水位类告警，过期/损坏告警由各自的流程处理
        int resolved = 0;
        for (InvAlert alert : openAlerts.values()) {
            if (alert.getAlertType() == null
                    || alert.getAlertType() < ALERT_TYPE_LOW_STOCK
                    || alert.getAlertType() > ALERT_TYPE_OUT_OF_STOCK) {
                continue;
            }
            alert.setIsResolved(true);
            alert.setResolvedAt(now);
            alert.setResolvedBy("SYSTEM");
            alert.setUpdateTime(now);
            alert.setRemark("库存已恢复，系统自动解除");
            alertMapper.updateById(alert);
            resolved++;
        }

        log.info("库存告警扫描完成: scanned={}, created={}, updated={}, resolved={}",
                inventories.size(), created, updated, resolved);
        notifyNewAlertsAfterCommit(createdAlerts);
        return InvAlertScanResult.builder()
                .scannedInventories(inventories.size())
                .created(created)
                .updated(updated)
                .resolved(resolved)
                .build();
    }

    /** Publishing before the commit would leak a notification for an alert that never landed. */
    private void notifyNewAlertsAfterCommit(List<InvAlert> alerts) {
        if (alerts.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifyNewAlerts(alerts);
                }
            });
        } else {
            notifyNewAlerts(alerts);
        }
    }

    private void notifyNewAlerts(List<InvAlert> alerts) {
        LocalDateTime now = LocalDateTime.now();
        for (InvAlert alert : alerts) {
            boolean sent;
            try {
                sent = notificationPublisher.publish(alert);
            } catch (RuntimeException e) {
                log.warn("Inventory alert {} was created but its notification failed", alert.getId(), e);
                sent = false;
            }
            if (sent) {
                alert.setNotified(true);
                alert.setNotifiedAt(now);
                alert.setUpdateTime(now);
                alertMapper.updateById(alert);
            }
        }
    }

    private Integer detectAlertType(Inventory inventory) {
        Integer available = inventory.getAvailableStock();
        if (available == null) {
            return null;
        }
        if (available <= 0) {
            return ALERT_TYPE_OUT_OF_STOCK;
        }
        Integer safety = inventory.getSafetyStock();
        if (safety != null && safety > 0 && available <= safety) {
            return ALERT_TYPE_LOW_STOCK;
        }
        return null;
    }

    private Integer thresholdOf(int alertType, Inventory inventory) {
        return alertType == ALERT_TYPE_OUT_OF_STOCK ? 0 : inventory.getSafetyStock();
    }

    private String alertKey(String tenantId, String skuId, String warehouseId) {
        return tenantId + "|" + skuId + "|" + warehouseId;
    }

    private String alertKey(String tenantId, String skuId, String warehouseId, Integer alertType) {
        return alertKey(tenantId, skuId, warehouseId) + "|" + alertType;
    }
}
