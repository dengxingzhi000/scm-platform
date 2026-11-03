package com.scmcloud.logistics.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.logistics.domain.entity.TmsCarrier;
import com.scmcloud.logistics.mapper.TmsCarrierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsCarrierQueryService {

    private final TmsCarrierMapper tmsCarrierMapper;

    @Slave
    public TmsCarrier getById(String id) {
        return tmsCarrierMapper.selectById(id);
    }

    @Slave
    public List<TmsCarrier> listAll() {
        return tmsCarrierMapper.selectList(null);
    }

    @Slave
    public Page<TmsCarrier> pageQuery(Page<TmsCarrier> page, Wrapper<TmsCarrier> wrapper) {
        return tmsCarrierMapper.selectPage(page, wrapper);
    }

    @Slave
    public Page<TmsCarrier> pageList(int page, int size, String carrierName, Integer carrierType, Boolean enabled) {
        log.debug("查询物流商列表: page={}, size={}, carrierName={}, carrierType={}, enabled={}", page, size, carrierName, carrierType, enabled);
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
        return tmsCarrierMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<TmsCarrier> listEnabled() {
        log.debug("查询已启用的物流商列表");
        LambdaQueryWrapper<TmsCarrier> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsCarrier::getEnabled, true);
        wrapper.eq(TmsCarrier::getDeleted, false);
        wrapper.orderByAsc(TmsCarrier::getSortOrder);
        return tmsCarrierMapper.selectList(wrapper);
    }

    @Slave
    public TmsCarrier getByCarrierCode(String carrierCode) {
        log.debug("根据物流商编码查询: carrierCode={}", carrierCode);
        LambdaQueryWrapper<TmsCarrier> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsCarrier::getCarrierCode, carrierCode);
        wrapper.eq(TmsCarrier::getDeleted, false);
        return tmsCarrierMapper.selectOne(wrapper);
    }
}
