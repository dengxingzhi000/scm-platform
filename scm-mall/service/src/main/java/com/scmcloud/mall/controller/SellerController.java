package com.scmcloud.mall.controller;

import com.scmcloud.mall.domain.entity.Seller;
import com.scmcloud.mall.service.ISellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/mall/sellers")
public class SellerController {

    private final ISellerService sellerService;

    @PostMapping
    public Seller registerSeller(@RequestBody Seller seller) {
        log.info("[API] Register seller: name={}", seller.getSellerName());
        return sellerService.registerSeller(seller);
    }

    @GetMapping("/{sellerId}")
    public Seller getSeller(@PathVariable Long sellerId) {
        return sellerService.getSeller(sellerId);
    }
}
