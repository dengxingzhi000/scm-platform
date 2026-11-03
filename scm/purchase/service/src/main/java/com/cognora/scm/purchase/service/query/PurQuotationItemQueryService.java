package com.cognora.scm.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.purchase.domain.entity.PurQuotationItem;
import com.cognora.scm.purchase.mapper.PurQuotationItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurQuotationItemQueryService {

    private final PurQuotationItemMapper purQuotationItemMapper;

    @Slave
    public PurQuotationItem getById(String id) {
        return purQuotationItemMapper.selectById(id);
    }

    @Slave
    public List<PurQuotationItem> listAll() {
        return purQuotationItemMapper.selectList(null);
    }

    @Slave
    public Page<PurQuotationItem> pageQuery(Page<PurQuotationItem> page, Wrapper<PurQuotationItem> wrapper) {
        return purQuotationItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<PurQuotationItem> listByQuotationId(String quotationId) {
        LambdaQueryWrapper<PurQuotationItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurQuotationItem::getQuotationId, quotationId);
        wrapper.orderByAsc(PurQuotationItem::getCreateTime);
        return purQuotationItemMapper.selectList(wrapper);
    }
}
