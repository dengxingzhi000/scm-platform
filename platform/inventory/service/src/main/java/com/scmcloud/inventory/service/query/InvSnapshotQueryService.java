package com.scmcloud.inventory.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.inventory.domain.entity.InvSnapshot;
import com.scmcloud.inventory.dto.InvSnapshotQueryRequest;
import com.scmcloud.inventory.mapper.InvSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvSnapshotQueryService {

    private final InvSnapshotMapper snapshotMapper;

    @Slave
    public InvSnapshot getById(String id) {
        return snapshotMapper.selectById(id);
    }

    @Slave
    public List<InvSnapshot> list() {
        return snapshotMapper.selectList(Wrappers.emptyWrapper());
    }

    @Slave
    public Page<InvSnapshot> page(int page, int size) {
        return snapshotMapper.selectPage(new Page<>(page, size), Wrappers.emptyWrapper());
    }

    @Slave
    public Page<InvSnapshot> query(InvSnapshotQueryRequest request) {
        LambdaQueryWrapper<InvSnapshot> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(request.getSnapshotDate() != null, InvSnapshot::getSnapshotDate, request.getSnapshotDate())
                .ge(request.getStartDate() != null, InvSnapshot::getSnapshotDate, request.getStartDate())
                .le(request.getEndDate() != null, InvSnapshot::getSnapshotDate, request.getEndDate())
                .eq(StringUtils.hasText(request.getSkuId()), InvSnapshot::getSkuId, request.getSkuId())
                .eq(StringUtils.hasText(request.getWarehouseId()), InvSnapshot::getWarehouseId, request.getWarehouseId())
                .orderByDesc(InvSnapshot::getSnapshotDate)
                .orderByAsc(InvSnapshot::getSkuId);
        return snapshotMapper.selectPage(new Page<>(request.getPage(), request.getSize()), wrapper);
    }

    /**
     * 查询指定 SKU/仓库最近 {@code days} 天的快照，用于绘制库存水位趋势。
     */
    @Slave
    public List<InvSnapshot> trend(String skuId, String warehouseId, int days) {
        LocalDate from = LocalDate.now().minusDays(Math.max(days, 1));
        LambdaQueryWrapper<InvSnapshot> wrapper = Wrappers.<InvSnapshot>lambdaQuery()
                .eq(StringUtils.hasText(skuId), InvSnapshot::getSkuId, skuId)
                .eq(StringUtils.hasText(warehouseId), InvSnapshot::getWarehouseId, warehouseId)
                .ge(InvSnapshot::getSnapshotDate, from)
                .orderByAsc(InvSnapshot::getSnapshotDate);
        return snapshotMapper.selectList(wrapper);
    }

    @Slave
    public long count() {
        return snapshotMapper.selectCount(Wrappers.emptyWrapper());
    }
}
