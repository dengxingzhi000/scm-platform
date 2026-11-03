package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurPriceComparison;
import com.scmcloud.purchase.mapper.PurPriceComparisonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPriceComparisonQueryService {

    private final PurPriceComparisonMapper purPriceComparisonMapper;

    @Slave
    public PurPriceComparison getById(String id) {
        return purPriceComparisonMapper.selectById(id);
    }

    @Slave
    public List<PurPriceComparison> listAll() {
        return purPriceComparisonMapper.selectList(null);
    }

    @Slave
    public Page<PurPriceComparison> pageQuery(Page<PurPriceComparison> page, Wrapper<PurPriceComparison> wrapper) {
        return purPriceComparisonMapper.selectPage(page, wrapper);
    }

    @Slave
    public PurPriceComparison getByComparisonNo(String comparisonNo) {
        LambdaQueryWrapper<PurPriceComparison> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurPriceComparison::getComparisonNo, comparisonNo);
        wrapper.eq(PurPriceComparison::getDeleted, false);
        return purPriceComparisonMapper.selectOne(wrapper);
    }

    @Slave
    public Page<PurPriceComparison> pageQuery(int page, int size, Integer status, String rfqId) {
        LambdaQueryWrapper<PurPriceComparison> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurPriceComparison::getStatus, status);
        }
        if (StringUtils.hasText(rfqId)) {
            wrapper.eq(PurPriceComparison::getRfqId, rfqId);
        }
        wrapper.eq(PurPriceComparison::getDeleted, false);
        wrapper.orderByDesc(PurPriceComparison::getCreateTime);
        return purPriceComparisonMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<PurPriceComparison> listByRfqId(String rfqId) {
        LambdaQueryWrapper<PurPriceComparison> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurPriceComparison::getRfqId, rfqId);
        wrapper.eq(PurPriceComparison::getDeleted, false);
        wrapper.orderByDesc(PurPriceComparison::getCreateTime);
        return purPriceComparisonMapper.selectList(wrapper);
    }
}
