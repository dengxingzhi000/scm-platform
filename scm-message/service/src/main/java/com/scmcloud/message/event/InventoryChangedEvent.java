package com.scmcloud.message.event;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class InventoryChangedEvent implements DomainEvent {
    private String eventId;
    private String skuId;
    private String warehouseId;
    private Integer quantityChange;
    private Integer newQuantity;
    private String changeType;
    private Long tenantId;
    private Date timestamp;
    
    @Override
    public String getEventType() {
        return "INVENTORY_CHANGED";
    }
    
    @Override
    public String getAggregateType() {
        return "Inventory";
    }
    
    @Override
    public String getAggregateId() {
        return skuId + ":" + warehouseId;
    }
    
    @Override
    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", skuId);
        payload.put("warehouseId", warehouseId);
        payload.put("quantityChange", quantityChange);
        payload.put("newQuantity", newQuantity);
        payload.put("changeType", changeType);
        return payload;
    }
    
    public static InventoryChangedEvent of(String skuId, String warehouseId, 
                                            Integer quantityChange, Integer newQuantity,
                                            String changeType, Long tenantId) {
        return InventoryChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .skuId(skuId)
                .warehouseId(warehouseId)
                .quantityChange(quantityChange)
                .newQuantity(newQuantity)
                .changeType(changeType)
                .tenantId(tenantId)
                .timestamp(new Date())
                .build();
    }
}
