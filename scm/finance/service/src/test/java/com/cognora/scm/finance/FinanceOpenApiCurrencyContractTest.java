package com.cognora.scm.finance;

import com.cognora.scm.finance.domain.dto.CreateInvoiceRequest;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 币种字段的契约一致性：OpenAPI 快照里的 schema 必须与后端实体/请求 DTO 同步暴露
 * {@code currency}，否则前端生成的客户端拿不到币种。
 */
class FinanceOpenApiCurrencyContractTest {

    private static final Path SNAPSHOT =
            Path.of("../../../docs/api/openapi-scm-finance.json");

    @Test
    void settlementOrderAndInvoiceSchemasExposeCurrency() throws Exception {
        assertTrue(Files.exists(SNAPSHOT), "缺少 OpenAPI 快照: " + SNAPSHOT.toAbsolutePath());
        JsonNode schemas = new ObjectMapper()
                .readTree(Files.readString(SNAPSHOT))
                .path("components")
                .path("schemas");

        for (String schema : new String[]{"SettlementOrder", "Invoice", "CreateInvoiceRequest"}) {
            JsonNode properties = schemas.path(schema).path("properties");
            assertTrue(properties.has("currency"), schema + " schema 缺少 currency");
            assertTrue(properties.path("currency").path("type").asText().equals("string"),
                    schema + " 的 currency 必须是 string");
        }
    }

    @Test
    void backendTypesDeclareCurrency() throws Exception {
        for (Class<?> type : new Class<?>[]{SettlementOrder.class, Invoice.class, CreateInvoiceRequest.class}) {
            Field currency = type.getDeclaredField("currency");
            assertTrue(currency.getType() == String.class, type.getSimpleName() + " 的 currency 必须是 String");
        }
    }
}
