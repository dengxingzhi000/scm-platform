package com.scmcloud.mall.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.mall.domain.entity.Seller;
import com.scmcloud.mall.mapper.SellerMapper;
import com.scmcloud.mall.service.ISellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class SellerServiceImpl extends ServiceImpl<SellerMapper, Seller> implements ISellerService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Seller registerSeller(Seller seller) {
        if (seller == null || seller.getSellerName() == null || seller.getSellerName().isBlank()) {
            throw new IllegalArgumentException("sellerName is required");
        }
        LocalDateTime now = LocalDateTime.now();
        seller.setStatus(1);
        seller.setCreatedAt(now);
        seller.setUpdatedAt(now);
        save(seller);
        log.info("Seller registered: sellerId={}, name={}", seller.getId(), seller.getSellerName());
        return seller;
    }

    @Override
    public Seller getSeller(Long sellerId) {
        return getById(sellerId);
    }
}
