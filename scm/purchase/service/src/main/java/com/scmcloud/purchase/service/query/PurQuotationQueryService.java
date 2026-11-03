package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurQuotation;
import com.scmcloud.purchase.mapper.PurQuotationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurQuotationQueryService {

    private final PurQuotationMapper purQuotationMapper;

    @Slave
    public PurQuotation getById(String id) {
        return purQuotationMapper.selectById(id);
    }

    @Slave
    public List<PurQuotation> listAll() {
        return purQuotationMapper.selectList(null);
    }

    @Slave
    public Page<PurQuotation> pageQuery(Page<PurQuotation> page, Wrapper<PurQuotation> wrapper) {
        return purQuotationMapper.selectPage(page, wrapper);
    }

    @Slave
    public PurQuotation getByQuotationNo(String quotationNo) {
        LambdaQueryWrapper<PurQuotation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurQuotation::getQuotationNo, quotationNo);
        wrapper.eq(PurQuotation::getDeleted, false);
        return purQuotationMapper.selectOne(wrapper);
    }

    @Slave
    public Page<PurQuotation> pageQuery(int page, int size, Integer status, String supplierId, String rfqId) {
        LambdaQueryWrapper<PurQuotation> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurQuotation::getStatus, status);
        }
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(PurQuotation::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(rfqId)) {
            wrapper.eq(PurQuotation::getRfqId, rfqId);
        }
        wrapper.eq(PurQuotation::getDeleted, false);
        wrapper.orderByDesc(PurQuotation::getCreateTime);
        return purQuotationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<PurQuotation> listByRfqId(String rfqId) {
        LambdaQueryWrapper<PurQuotation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurQuotation::getRfqId, rfqId);
        wrapper.eq(PurQuotation::getDeleted, false);
        wrapper.orderByDesc(PurQuotation::getCreateTime);
        return purQuotationMapper.selectList(wrapper);
    }

    @Slave
    public List<PurQuotation> listBySupplierId(String supplierId) {
        LambdaQueryWrapper<PurQuotation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurQuotation::getSupplierId, supplierId);
        wrapper.eq(PurQuotation::getDeleted, false);
        wrapper.orderByDesc(PurQuotation::getCreateTime);
        return purQuotationMapper.selectList(wrapper);
    }
}
