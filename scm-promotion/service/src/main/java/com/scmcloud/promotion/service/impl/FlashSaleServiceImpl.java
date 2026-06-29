package com.scmcloud.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.promotion.domain.entity.FlashSale;
import com.scmcloud.promotion.mapper.FlashSaleMapper;
import com.scmcloud.promotion.service.IFlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
@Service
public class FlashSaleServiceImpl extends ServiceImpl<FlashSaleMapper, FlashSale> implements IFlashSaleService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String FLASH_STOCK_KEY = "flash_sale:stock:";

    @Override
    public List<FlashSale> getActiveFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        return list(new LambdaQueryWrapper<FlashSale>()
                .eq(FlashSale::getStatus, 1)
                .le(FlashSale::getStartTime, now)
                .ge(FlashSale::getEndTime, now));
    }

    @Override
    public boolean checkStock(Long flashSaleId, Long skuId) {
        String key = FLASH_STOCK_KEY + flashSaleId + ":" + skuId;
        Object stock = redisTemplate.opsForValue().get(key);
        if (stock != null) {
            return Integer.parseInt(stock.toString()) > 0;
        }

        FlashSale flashSale = getById(flashSaleId);
        if (flashSale == null || !flashSale.getSkuId().equals(skuId)) {
            return false;
        }

        int remaining = flashSale.getFlashStock() - flashSale.getSoldCount();
        redisTemplate.opsForValue().set(key, String.valueOf(remaining), 30, TimeUnit.SECONDS);
        return remaining > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long flashSaleId, Long skuId, Integer quantity) {
        String key = FLASH_STOCK_KEY + flashSaleId + ":" + skuId;

        Long remaining = redisTemplate.opsForValue().decrement(key, quantity);
        if (remaining == null || remaining < 0) {
            if (remaining != null) {
                redisTemplate.opsForValue().increment(key, quantity);
            }
            return false;
        }

        FlashSale flashSale = getById(flashSaleId);
        if (flashSale == null) {
            return false;
        }

        flashSale.setSoldCount(flashSale.getSoldCount() + quantity);
        updateById(flashSale);

        log.info("Flash sale stock deducted: flashSaleId={}, skuId={}, quantity={}", flashSaleId, skuId, quantity);
        return true;
    }

    @Override
    public FlashSale getByProductAndSku(Long productId, Long skuId) {
        LocalDateTime now = LocalDateTime.now();
        return getOne(new LambdaQueryWrapper<FlashSale>()
                .eq(FlashSale::getProductId, productId)
                .eq(FlashSale::getSkuId, skuId)
                .eq(FlashSale::getStatus, 1)
                .le(FlashSale::getStartTime, now)
                .ge(FlashSale::getEndTime, now));
    }
}
