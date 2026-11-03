package com.cognora.platform.inventory.service.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.platform.inventory.domain.entity.InvSnapshot;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InvSnapshotGenerationResult;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvSnapshotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvSnapshotCommandService {

    private final InvSnapshotMapper snapshotMapper;
    private final InvInventoryMapper inventoryMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(InvSnapshot snapshot) {
        return snapshotMapper.insert(snapshot);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(InvSnapshot snapshot) {
        return snapshotMapper.updateById(snapshot);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return snapshotMapper.deleteById(id);
    }

    /**
     * 为指定日期生成全量库存快照（按租户 + SKU + 仓库去重，可重复执行）。
     *
     * <p>定时任务无租户上下文时覆盖全部租户，HTTP 手动触发时只覆盖当前租户。</p>
     */
    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public InvSnapshotGenerationResult generateDailySnapshots(LocalDate snapshotDate) {
        List<Inventory> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getDeleted, false));
        List<InvSnapshot> existing = snapshotMapper.selectList(
                new LambdaQueryWrapper<InvSnapshot>().eq(InvSnapshot::getSnapshotDate, snapshotDate));

        Set<String> existingKeys = new HashSet<>();
        for (InvSnapshot snapshot : existing) {
            existingKeys.add(snapshotKey(snapshot.getTenantId(), snapshot.getSkuId(), snapshot.getWarehouseId()));
        }

        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (Inventory inventory : inventories) {
            String key = snapshotKey(inventory.getTenantId(), inventory.getSkuId(), inventory.getWarehouseId());
            if (existingKeys.contains(key)) {
                skipped++;
                continue;
            }
            try {
                InvSnapshot snapshot = new InvSnapshot();
                snapshot.setId(UUID.randomUUID().toString());
                snapshot.setTenantId(inventory.getTenantId());
                snapshot.setSnapshotDate(snapshotDate);
                snapshot.setSkuId(inventory.getSkuId());
                snapshot.setWarehouseId(inventory.getWarehouseId());
                snapshot.setTotalStock(nullSafe(inventory.getTotalStock()));
                snapshot.setAvailableStock(nullSafe(inventory.getAvailableStock()));
                snapshot.setLockedStock(nullSafe(inventory.getLockedStock()));
                snapshot.setDamagedStock(nullSafe(inventory.getDamagedStock()));
                snapshot.setAverageCost(inventory.getAverageCost());
                snapshot.setTotalValue(totalValue(inventory));
                snapshot.setCreateTime(now);
                snapshotMapper.insert(snapshot);
                existingKeys.add(key);
                created++;
            } catch (Exception e) {
                failed++;
                log.error("库存快照生成失败: skuId={}, warehouseId={}", inventory.getSkuId(), inventory.getWarehouseId(), e);
            }
        }

        log.info("库存快照生成完成: date={}, scanned={}, created={}, skipped={}, failed={}",
                snapshotDate, inventories.size(), created, skipped, failed);
        return InvSnapshotGenerationResult.builder()
                .snapshotDate(snapshotDate)
                .scannedInventories(inventories.size())
                .created(created)
                .skipped(skipped)
                .failed(failed)
                .build();
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal totalValue(Inventory inventory) {
        if (inventory.getAverageCost() == null) {
            return BigDecimal.ZERO;
        }
        return inventory.getAverageCost().multiply(BigDecimal.valueOf(nullSafe(inventory.getTotalStock())));
    }

    private String snapshotKey(String tenantId, String skuId, String warehouseId) {
        return tenantId + "|" + skuId + "|" + warehouseId;
    }
}
