package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurPriceComparisonItem;
import com.scmcloud.purchase.mapper.PurPriceComparisonItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPriceComparisonItemQueryService {

    private final PurPriceComparisonItemMapper purPriceComparisonItemMapper;

    @Slave
    public PurPriceComparisonItem getById(String id) {
        return purPriceComparisonItemMapper.selectById(id);
    }

    @Slave
    public List<PurPriceComparisonItem> listAll() {
        return purPriceComparisonItemMapper.selectList(null);
    }

    @Slave
    public Page<PurPriceComparisonItem> pageQuery(Page<PurPriceComparisonItem> page, Wrapper<PurPriceComparisonItem> wrapper) {
        return purPriceComparisonItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<PurPriceComparisonItem> listByComparisonId(String comparisonId) {
        LambdaQueryWrapper<PurPriceComparisonItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurPriceComparisonItem::getComparisonId, comparisonId);
        wrapper.orderByAsc(PurPriceComparisonItem::getRank);
        return purPriceComparisonItemMapper.selectList(wrapper);
    }
}
