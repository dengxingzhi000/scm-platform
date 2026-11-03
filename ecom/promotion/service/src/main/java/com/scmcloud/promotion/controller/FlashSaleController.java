package com.scmcloud.promotion.controller;

import com.scmcloud.promotion.domain.entity.FlashSale;
import com.scmcloud.promotion.service.IFlashSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/promotions/flash-sales")
@Tag(name = "秒杀活动", description = "进行中秒杀活动的查询、库存校验与下单扣减库存接口。")
public class FlashSaleController {

    private final IFlashSaleService flashSaleService;

    @Operation(summary = "查询进行中的秒杀", description = "获取当前正在进行中的全部秒杀活动。")
    @GetMapping("/active")
    public List<FlashSale> getActiveFlashSales() {
        log.info("[API] Get active flash sales");
        return flashSaleService.getActiveFlashSales();
    }

    @Operation(summary = "校验秒杀库存", description = "判断指定秒杀活动下某 SKU 是否仍有可售库存。")
    @PostMapping("/check-stock")
    public boolean checkStock(@RequestBody CheckStockRequest request) {
        log.info("[API] Check flash sale stock: flashSaleId={}, skuId={}", request.getFlashSaleId(), request.getSkuId());
        return flashSaleService.checkStock(request.getFlashSaleId(), request.getSkuId());
    }

    @Operation(summary = "秒杀下单", description = "按指定数量扣减秒杀活动 SKU 库存并返回扣减结果。")
    @PostMapping("/purchase")
    public boolean purchase(@RequestBody PurchaseRequest request) {
        log.info("[API] Purchase flash sale: flashSaleId={}, skuId={}, quantity={}",
                request.getFlashSaleId(), request.getSkuId(), request.getQuantity());
        return flashSaleService.deductStock(request.getFlashSaleId(), request.getSkuId(), request.getQuantity());
    }

    @Data
    public static class CheckStockRequest {
        private Long flashSaleId;
        private Long skuId;
    }

    @Data
    public static class PurchaseRequest {
        private Long flashSaleId;
        private Long skuId;
        private Integer quantity;
    }
}
