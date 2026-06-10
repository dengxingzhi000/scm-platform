package com.scmcloud.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.scmcloud.logistics.domain.entity.TmsCarrier;
import com.scmcloud.logistics.mapper.TmsCarrierMapper;
import com.scmcloud.logistics.service.ITmsCarrierService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TmsCarrierServiceImpl extends ServiceImpl<TmsCarrierMapper, TmsCarrier> implements ITmsCarrierService {

    @Override
    public Page<TmsCarrier> pageList(int page, int size, String carrierName, Integer carrierType, Boolean enabled) {
        log.debug("Query carrier list: page={}, size={}, carrierName={}, carrierType={}, enabled={}", page, size, carrierName, carrierType, enabled);

        LambdaQueryWrapper<TmsCarrier> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(carrierName)) {
            wrapper.like(TmsCarrier::getCarrierName, carrierName);
        }
        if (carrierType != null) {
            wrapper.eq(TmsCarrier::getCarrierType, carrierType);
        }
        if (enabled != null) {
            wrapper.eq(TmsCarrier::getEnabled, enabled);
        }
        wrapper.eq(TmsCarrier::getDeleted, false);
        wrapper.orderByAsc(TmsCarrier::getSortOrder);
        wrapper.orderByDesc(TmsCarrier::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<TmsCarrier> listEnabled() {
        log.debug("Query enabled carriers list");
        return lambdaQuery()
                .eq(TmsCarrier::getEnabled, true)
                .eq(TmsCarrier::getDeleted, false)
                .orderByAsc(TmsCarrier::getSortOrder)
                .list();
    }

    @Override
    public TmsCarrier getByCarrierCode(String carrierCode) {
        log.debug("Query carrier by code: carrierCode={}", carrierCode);
        return lambdaQuery()
                .eq(TmsCarrier::getCarrierCode, carrierCode)
                .eq(TmsCarrier::getDeleted, false)
                .one();
    }
}
