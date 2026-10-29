package com.scmcloud.mall.controller;

import com.scmcloud.mall.domain.entity.Seller;
import com.scmcloud.mall.service.ISellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "卖家管理", description = "商城卖家的入驻注册与信息查询接口。")
public class SellerController {

    private final ISellerService sellerService;

    @Operation(summary = "卖家入驻", description = "注册新的商城卖家并返回注册后的卖家信息。")
    @PostMapping
    public Seller registerSeller(@RequestBody Seller seller) {
        log.info("[API] Register seller: name={}", (seller.getSellerName() + "").replace("\r", "_").replace("\n", "_"));
        return sellerService.registerSeller(seller);
    }

    @Operation(summary = "查询卖家信息", description = "根据卖家 ID 获取卖家的详细信息。")
    @GetMapping("/{sellerId}")
    public Seller getSeller(@Parameter(description = "卖家 ID") @PathVariable Long sellerId) {
        return sellerService.getSeller(sellerId);
    }
}
