package com.scmcloud.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.scmcloud.promotion.domain.entity.Coupon;
import com.scmcloud.promotion.domain.entity.CouponTemplate;
import com.scmcloud.promotion.mapper.CouponMapper;
import com.scmcloud.promotion.mapper.CouponTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock private CouponMapper couponMapper;
    @Mock private CouponTemplateMapper couponTemplateMapper;

    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl(couponTemplateMapper);
        ReflectionTestUtils.setField(couponService, "baseMapper", couponMapper);
    }

    @Test
    void issueCouponRejectsInactiveTemplate() {
        when(couponTemplateMapper.selectById(2L)).thenReturn(template().setStatus(0));

        assertThrows(IllegalStateException.class,
                () -> couponService.issueCoupon("user-1", 2L));
        verify(couponMapper, never()).insert(any(Coupon.class));
    }

    @Test
    void issueCouponRejectsTemplateThatHasNotStarted() {
        when(couponTemplateMapper.selectById(2L))
                .thenReturn(template().setStartTime(LocalDateTime.now().plusDays(1)));

        assertThrows(IllegalStateException.class,
                () -> couponService.issueCoupon("user-1", 2L));
    }

    @Test
    void issueCouponFailsWhenAtomicReservationFindsNoStock() {
        when(couponTemplateMapper.selectById(2L)).thenReturn(template());
        when(couponTemplateMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> couponService.issueCoupon("user-1", 2L));
        verify(couponMapper, never()).insert(any(Coupon.class));
    }

    @Test
    void issueCouponReservesStockThenInsertsCoupon() {
        when(couponTemplateMapper.selectById(2L)).thenReturn(template());
        when(couponTemplateMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(couponMapper.insert(any(Coupon.class))).thenReturn(1);

        Coupon coupon = couponService.issueCoupon("user-1", 2L);

        assertEquals(1, coupon.getStatus());
        assertTrue(coupon.getCouponCode().length() >= 16);
    }

    @Test
    void useCouponIsIdempotentForSameOrder() {
        Coupon used = usedCoupon("ORD-1");
        when(couponMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(used);

        couponService.useCoupon("CODE-1", "ORD-1");

        verify(couponMapper, never()).updateById(any(Coupon.class));
    }

    @Test
    void useCouponRejectsReuseOnDifferentOrder() {
        Coupon used = usedCoupon("ORD-1");
        when(couponMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(used);

        assertThrows(IllegalStateException.class,
                () -> couponService.useCoupon("CODE-1", "ORD-2"));
    }

    @Test
    void useCouponMarksUnusedCouponAsUsed() {
        Coupon unused = new Coupon()
                .setCouponCode("CODE-1")
                .setStatus(1)
                .setCreatedAt(LocalDateTime.now());
        when(couponMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(unused);
        when(couponMapper.updateById(any(Coupon.class))).thenReturn(1);

        couponService.useCoupon("CODE-1", "ORD-1");

        assertEquals(2, unused.getStatus());
        assertEquals("ORD-1", unused.getOrderNo());
    }

    @Test
    void verifyCouponRejectsExpiredTemplate() {
        Coupon unused = new Coupon().setCouponCode("CODE-1").setTemplateId(1L).setStatus(1);
        when(couponMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(unused);
        when(couponTemplateMapper.selectById(1L))
                .thenReturn(template().setEndTime(LocalDateTime.now().minusDays(1)));

        assertThrows(IllegalStateException.class,
                () -> couponService.verifyCoupon("user-1", "CODE-1", new BigDecimal("100")));
    }

    @Test
    void verifyCouponRejectsOrderBelowMinimum() {
        Coupon unused = new Coupon().setCouponCode("CODE-1").setTemplateId(1L).setStatus(1);
        when(couponMapper.selectOne(any(Wrapper.class), eq(true))).thenReturn(unused);
        when(couponTemplateMapper.selectById(1L)).thenReturn(template());

        assertThrows(IllegalStateException.class,
                () -> couponService.verifyCoupon("user-1", "CODE-1", new BigDecimal("10")));
    }

    private CouponTemplate template() {
        return new CouponTemplate()
                .setId(1L)
                .setStatus(1)
                .setTotalCount(100)
                .setIssuedCount(10)
                .setMinAmount(new BigDecimal("50"));
    }

    private Coupon usedCoupon(String orderNo) {
        return new Coupon()
                .setCouponCode("CODE-1")
                .setStatus(2)
                .setOrderNo(orderNo);
    }
}
