package com.scmcloud.tenant.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.tenant.domain.entity.TenantPlatformFee;
import com.scmcloud.tenant.mapper.TenantPlatformFeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 平台服务费账单查询。
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@Service
@RequiredArgsConstructor
public class PlatformFeeQueryService {

    private final TenantPlatformFeeMapper feeMapper;

    public Page<TenantPlatformFee> page(int page, int size, String tenantId, String billingPeriod, Integer status) {
        LambdaQueryWrapper<TenantPlatformFee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.hasText(tenantId), TenantPlatformFee::getTenantId, tenantId)
                .eq(StringUtils.hasText(billingPeriod), TenantPlatformFee::getBillingPeriod, billingPeriod)
                .eq(status != null, TenantPlatformFee::getStatus, status)
                .orderByDesc(TenantPlatformFee::getBillingPeriod)
                .orderByDesc(TenantPlatformFee::getCreateTime);
        return feeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<TenantPlatformFee> listByTenant(String tenantId) {
        return feeMapper.selectList(Wrappers.<TenantPlatformFee>lambdaQuery()
                .eq(TenantPlatformFee::getTenantId, tenantId)
                .orderByDesc(TenantPlatformFee::getBillingPeriod));
    }
}
