package com.scmcloud.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.promotion.domain.entity.Coupon;
import com.scmcloud.promotion.domain.entity.CouponTemplate;
import com.scmcloud.promotion.mapper.CouponMapper;
import com.scmcloud.promotion.service.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final com.scmcloud.promotion.mapper.CouponTemplateMapper couponTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon issueCoupon(String userId, Long templateId) {
        log.info("Issuing coupon: userId={}, templateId={}", userId, templateId);

        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Coupon template not found: " + templateId);
        }

        if (template.getIssuedCount() >= template.getTotalCount()) {
            throw new IllegalStateException("Coupon template exhausted: " + templateId);
        }

        Coupon coupon = new Coupon();
        coupon.setTemplateId(templateId);
        coupon.setUserId(userId);
        coupon.setCouponCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        coupon.setStatus(1);
        coupon.setCreatedAt(LocalDateTime.now());

        save(coupon);

        template.setIssuedCount(template.getIssuedCount() + 1);
        couponTemplateMapper.updateById(template);

        log.info("Coupon issued: couponCode={}", coupon.getCouponCode());
        return coupon;
    }

    @Override
    public Coupon verifyCoupon(String userId, String couponCode, BigDecimal orderAmount) {
        Coupon coupon = getOne(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCouponCode, couponCode)
                .eq(Coupon::getUserId, userId)
                .eq(Coupon::getStatus, 1));

        if (coupon == null) {
            throw new IllegalArgumentException("Coupon not found or already used: " + couponCode);
        }

        CouponTemplate template = couponTemplateMapper.selectById(coupon.getTemplateId());
        if (template == null || template.getStatus() != 1) {
            throw new IllegalStateException("Coupon template is inactive");
        }

        if (orderAmount.compareTo(template.getMinAmount()) < 0) {
            throw new IllegalStateException("Order amount does not meet minimum requirement: " + template.getMinAmount());
        }

        return coupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(String couponCode, String orderNo) {
        Coupon coupon = getOne(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCouponCode, couponCode));

        if (coupon == null) {
            throw new IllegalArgumentException("Coupon not found: " + couponCode);
        }

        coupon.setStatus(2);
        coupon.setUsedAt(LocalDateTime.now());
        coupon.setOrderNo(orderNo);
        updateById(coupon);

        log.info("Coupon used: couponCode={}, orderNo={}", couponCode, orderNo);
    }

    @Override
    public List<Coupon> getByUserId(String userId) {
        return list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getUserId, userId)
                .orderByDesc(Coupon::getCreatedAt));
    }
}
