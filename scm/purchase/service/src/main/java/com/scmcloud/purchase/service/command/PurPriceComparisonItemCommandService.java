package com.scmcloud.purchase.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurPriceComparisonItem;
import com.scmcloud.purchase.mapper.PurPriceComparisonItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPriceComparisonItemCommandService {

    private final PurPriceComparisonItemMapper purPriceComparisonItemMapper;

    @Master(reason = "保存比价分析明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurPriceComparisonItem entity) {
        return purPriceComparisonItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新比价分析明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurPriceComparisonItem entity) {
        return purPriceComparisonItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除比价分析明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purPriceComparisonItemMapper.deleteById(id) > 0;
    }

    @Master(reason = "根据比价ID删除明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByComparisonId(String comparisonId) {
        LambdaUpdateWrapper<PurPriceComparisonItem> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(PurPriceComparisonItem::getComparisonId, comparisonId);
        return purPriceComparisonItemMapper.delete(wrapper) > 0;
    }
}
