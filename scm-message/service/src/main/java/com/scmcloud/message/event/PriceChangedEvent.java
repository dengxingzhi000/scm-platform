package com.scmcloud.message.event;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class PriceChangedEvent implements DomainEvent {
    private String eventId;
    private String productId;
    private String skuId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String priceListId;
    private Long tenantId;
    private Date timestamp;
    
    @Override
    public String getEventType() {
        return "PRICE_CHANGED";
    }
    
    @Override
    public String getAggregateType() {
        return "Price";
    }
    
    @Override
    public String getAggregateId() {
        return productId + ":" + skuId;
    }
    
    @Override
    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("skuId", skuId);
        payload.put("oldPrice", oldPrice);
        payload.put("newPrice", newPrice);
        payload.put("priceListId", priceListId);
        return payload;
    }
    
    public static PriceChangedEvent of(String productId, String skuId, 
                                        BigDecimal oldPrice, BigDecimal newPrice,
                                        String priceListId, Long tenantId) {
        return PriceChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .productId(productId)
                .skuId(skuId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .priceListId(priceListId)
                .tenantId(tenantId)
                .timestamp(new Date())
                .build();
    }
}
