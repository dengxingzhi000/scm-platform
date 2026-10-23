package com.scmcloud.promotion.service.dubbo;

import com.scmcloud.promotion.api.PromotionDubboService;
import com.scmcloud.promotion.api.dto.CouponVO;
import com.scmcloud.promotion.domain.entity.Coupon;
import com.scmcloud.promotion.domain.entity.CouponTemplate;
import com.scmcloud.promotion.mapper.CouponTemplateMapper;
import com.scmcloud.promotion.service.ICouponService;
import com.scmcloud.promotion.service.IFlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class PromotionDubboServiceImpl implements PromotionDubboService {

    private final ICouponService couponService;
    private final IFlashSaleService flashSaleService;
    private final CouponTemplateMapper couponTemplateMapper;

    @Override
    public CouponVO issueCoupon(Long userId, Long templateId) {
        return toVO(couponService.issueCoupon(String.valueOf(userId), templateId));
    }

    @Override
    public CouponVO verifyCoupon(Long userId, String couponCode, BigDecimal orderAmount) {
        return toVO(couponService.verifyCoupon(String.valueOf(userId), couponCode, orderAmount));
    }

    @Override
    public void useCoupon(String couponCode, String orderNo) {
        couponService.useCoupon(couponCode, orderNo);
    }

    @Override
    public List<CouponVO> getUserCoupons(Long userId) {
        return couponService.getByUserId(String.valueOf(userId)).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public boolean checkFlashSaleStock(Long flashSaleId, Long skuId) {
        return flashSaleService.checkStock(flashSaleId, skuId);
    }

    @Override
    public boolean deductFlashSaleStock(Long flashSaleId, Long skuId, Integer quantity) {
        return flashSaleService.deductStock(flashSaleId, skuId, quantity);
    }

    private CouponVO toVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setTemplateId(coupon.getTemplateId());
        vo.setUserId(coupon.getUserId());
        vo.setCouponCode(coupon.getCouponCode());
        vo.setStatus(coupon.getStatus());
        vo.setUsedAt(coupon.getUsedAt());
        vo.setOrderNo(coupon.getOrderNo());
        vo.setCreatedAt(coupon.getCreatedAt());
        CouponTemplate template = coupon.getTemplateId() == null
                ? null : couponTemplateMapper.selectById(coupon.getTemplateId());
        if (template != null) {
            vo.setDiscountValue(template.getDiscountValue());
            vo.setMinAmount(template.getMinAmount());
        }
        return vo;
    }
}
