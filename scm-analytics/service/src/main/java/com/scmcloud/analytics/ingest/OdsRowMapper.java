package com.scmcloud.analytics.ingest;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps inbound envelope payload JSON to an ODS row ready for ClickHouse.
 * <p>
 * Pure data transformation — no Redis, no Kafka, no IO.
 */
public final class OdsRowMapper {

    private OdsRowMapper() {
    }

    /**
     * Map an {@link OrderCreatedEvent}-shaped payload to the {@code ods_order}
     * table schema. Unknown / missing fields become {@code null}.
     *
     * @param topic        Kafka topic the record came from
     * @param innerPayload parsed JSON of the envelope's {@code data} field
     * @param kafkaOffset  the Kafka record offset
     * @param kafkaTsMillis the Kafka record timestamp (millis since epoch)
     */
    public static OdsRow orderFromPayload(String topic, JsonNode innerPayload, long kafkaOffset, long kafkaTsMillis) {
        Map<String, Object> row = new HashMap<>();
        row.put("tenant_id", textOrNull(innerPayload, "tenantId"));
        row.put("order_no", textOrNull(innerPayload, "orderNo"));
        row.put("order_id", textOrNull(innerPayload, "orderId"));
        row.put("user_id", textOrNull(innerPayload, "userId"));
        row.put("order_status", null);
        row.put("total_amount", decimalOrNull(innerPayload, "totalAmount"));
        row.put("pay_amount", decimalOrNull(innerPayload, "payableAmount"));
        row.put("pay_time", null);
        row.put("order_source", null);
        row.put("remark", null);
        Instant now = Instant.now();
        row.put("create_time", now);
        row.put("update_time", now);
        row.put("kafka_offset", kafkaOffset);
        row.put("kafka_timestamp", Instant.ofEpochMilli(kafkaTsMillis));
        row.put("_raw", innerPayload == null || innerPayload.isMissingNode() ? null : innerPayload.toString());
        return new OdsRow("ods_order", row);
    }

    /**
     * Map an {@link com.scmcloud.inventory.event.InventoryAdjustedEvent}-shaped
     * payload to the {@code ods_inventory} table schema.
     */
    public static OdsRow inventoryFromPayload(String topic, JsonNode innerPayload, long kafkaOffset, long kafkaTsMillis) {
        Map<String, Object> row = new HashMap<>();
        row.put("tenant_id", textOrNull(innerPayload, "tenantId"));
        row.put("inventory_id", null);
        row.put("sku_id", textOrNull(innerPayload, "skuId"));
        row.put("warehouse_id", textOrNull(innerPayload, "warehouseId"));
        row.put("location_id", null);
        row.put("change_type", "ADJUST");
        row.put("quantity", intOrNull(innerPayload, "quantity"));
        row.put("before_qty", null);
        row.put("after_qty", intOrNull(innerPayload, "availableStock"));
        row.put("biz_no", null);
        row.put("biz_type", "INVENTORY_ADJUSTED");
        Instant now = Instant.now();
        row.put("create_time", now);
        row.put("update_time", now);
        row.put("kafka_offset", kafkaOffset);
        row.put("kafka_timestamp", Instant.ofEpochMilli(kafkaTsMillis));
        row.put("_raw", innerPayload == null || innerPayload.isMissingNode() ? null : innerPayload.toString());
        return new OdsRow("ods_inventory", row);
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        return v.isInt() ? v.intValue() : Integer.parseInt(v.asText());
    }

    private static java.math.BigDecimal decimalOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        try {
            return new java.math.BigDecimal(v.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
