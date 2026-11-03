package com.scmcloud.purchase.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurQuotationItem;
import com.scmcloud.purchase.mapper.PurQuotationItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurQuotationItemCommandService {

    private final PurQuotationItemMapper purQuotationItemMapper;

    @Master(reason = "保存报价单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurQuotationItem entity) {
        return purQuotationItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新报价单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurQuotationItem entity) {
        return purQuotationItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除报价单明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purQuotationItemMapper.deleteById(id) > 0;
    }

    @Master(reason = "根据报价单ID删除明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByQuotationId(String quotationId) {
        LambdaUpdateWrapper<PurQuotationItem> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(PurQuotationItem::getQuotationId, quotationId);
        return purQuotationItemMapper.delete(wrapper) > 0;
    }
}
