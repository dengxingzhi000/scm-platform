package com.scmcloud.promotion.controller;

import com.scmcloud.promotion.domain.entity.FlashSale;
import com.scmcloud.promotion.service.IFlashSaleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/promotions/flash-sales")
public class FlashSaleController {

    private final IFlashSaleService flashSaleService;

    @GetMapping("/active")
    public List<FlashSale> getActiveFlashSales() {
        log.info("[API] Get active flash sales");
        return flashSaleService.getActiveFlashSales();
    }

    @PostMapping("/check-stock")
    public boolean checkStock(@RequestBody CheckStockRequest request) {
        log.info("[API] Check flash sale stock: flashSaleId={}, skuId={}", request.getFlashSaleId(), request.getSkuId());
        return flashSaleService.checkStock(request.getFlashSaleId(), request.getSkuId());
    }

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
