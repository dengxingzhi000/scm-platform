package com.scmcloud.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.promotion.domain.entity.Coupon;
import com.scmcloud.promotion.domain.entity.CouponTemplate;
import com.scmcloud.promotion.mapper.CouponMapper;
import com.scmcloud.promotion.service.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private static final int TEMPLATE_ACTIVE = 1;
    private static final int COUPON_UNUSED = 1;
    private static final int COUPON_USED = 2;

    private final com.scmcloud.promotion.mapper.CouponTemplateMapper couponTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon issueCoupon(String userId, Long templateId) {
        log.info("Issuing coupon: userId={}, templateId={}",
                (userId + "").replace("\r", "_").replace("\n", "_"),
                (templateId + "").replace("\r", "_").replace("\n", "_"));

        if (!StringUtils.hasText(userId) || templateId == null) {
            throw new IllegalArgumentException("userId and templateId are required");
        }

        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Coupon template not found: " + templateId);
        }
        if (template.getStatus() == null || template.getStatus() != TEMPLATE_ACTIVE) {
            throw new IllegalStateException("Coupon template is inactive: " + templateId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getStartTime() != null && now.isBefore(template.getStartTime())) {
            throw new IllegalStateException("Coupon template has not started: " + templateId);
        }
        if (template.getEndTime() != null && now.isAfter(template.getEndTime())) {
            throw new IllegalStateException("Coupon template has expired: " + templateId);
        }

        // Atomic reservation: concurrent issues cannot exceed total_count.
        int reserved = couponTemplateMapper.update(null,
                new LambdaUpdateWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getId, templateId)
                        .eq(CouponTemplate::getStatus, TEMPLATE_ACTIVE)
                        .apply("issued_count < total_count")
                        .setSql("issued_count = issued_count + 1"));
        if (reserved == 0) {
            throw new IllegalStateException("Coupon template exhausted: " + templateId);
        }

        Coupon coupon = new Coupon();
        coupon.setTemplateId(templateId);
        coupon.setUserId(userId);
        coupon.setCouponCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        coupon.setStatus(COUPON_UNUSED);
        coupon.setCreatedAt(now);
        save(coupon);

        log.info("Coupon issued: couponCode={}", coupon.getCouponCode());
        return coupon;
    }

    @Override
    public Coupon verifyCoupon(String userId, String couponCode, BigDecimal orderAmount) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(couponCode)) {
            throw new IllegalArgumentException("userId and couponCode are required");
        }
        Coupon coupon = getOne(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCouponCode, couponCode)
                .eq(Coupon::getUserId, userId)
                .eq(Coupon::getStatus, COUPON_UNUSED));

        if (coupon == null) {
            throw new IllegalArgumentException("Coupon not found or already used: " + couponCode);
        }

        CouponTemplate template = couponTemplateMapper.selectById(coupon.getTemplateId());
        if (template == null || template.getStatus() == null || template.getStatus() != TEMPLATE_ACTIVE) {
            throw new IllegalStateException("Coupon template is inactive");
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getStartTime() != null && now.isBefore(template.getStartTime())) {
            throw new IllegalStateException("Coupon is not effective yet");
        }
        if (template.getEndTime() != null && now.isAfter(template.getEndTime())) {
            throw new IllegalStateException("Coupon has expired");
        }

        BigDecimal minAmount = template.getMinAmount() == null ? BigDecimal.ZERO : template.getMinAmount();
        BigDecimal order = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        if (order.compareTo(minAmount) < 0) {
            throw new IllegalStateException("Order amount does not meet minimum requirement: " + minAmount);
        }

        return coupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(String couponCode, String orderNo) {
        if (!StringUtils.hasText(couponCode) || !StringUtils.hasText(orderNo)) {
            throw new IllegalArgumentException("couponCode and orderNo are required");
        }
        Coupon coupon = getOne(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCouponCode, couponCode));

        if (coupon == null) {
            throw new IllegalArgumentException("Coupon not found: " + couponCode);
        }
        if (coupon.getStatus() != null && coupon.getStatus() == COUPON_USED) {
            // Idempotent replay for the same order; a different order is a conflict.
            if (orderNo.equals(coupon.getOrderNo())) {
                log.info("Coupon already used by the same order: couponCode={}, orderNo={}", couponCode, orderNo);
                return;
            }
            throw new IllegalStateException("Coupon already used by order: " + coupon.getOrderNo());
        }
        if (coupon.getStatus() == null || coupon.getStatus() != COUPON_UNUSED) {
            throw new IllegalStateException("Coupon is not usable, status: " + coupon.getStatus());
        }

        coupon.setStatus(COUPON_USED);
        coupon.setUsedAt(LocalDateTime.now());
        coupon.setOrderNo(orderNo);
        updateById(coupon);

        log.info("Coupon used: couponCode={}, orderNo={}",
                (couponCode + "").replace("\r", "_").replace("\n", "_"),
                (orderNo + "").replace("\r", "_").replace("\n", "_"));
    }

    @Override
    public List<Coupon> getByUserId(String userId) {
        return list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getUserId, userId)
                .orderByDesc(Coupon::getCreatedAt));
    }

    @Override
    public Page<Coupon> pageQuery(int page, int size, String userId, Integer status) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(userId), Coupon::getUserId, userId)
                .eq(status != null, Coupon::getStatus, status)
                .orderByDesc(Coupon::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }
}
