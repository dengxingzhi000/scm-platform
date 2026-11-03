package com.scmcloud.common.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.security.config.JacksonConfig;
import com.scmcloud.common.web.annotation.Sensitive;
import com.scmcloud.common.web.enums.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JacksonConfigTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    public static class OrderPayload {
        private String orderNo = "ORD-2026-001";
        @Sensitive(type = SensitiveType.MOBILE)
        private String customerPhone = "13800138001";
        private LocalDateTime createTime = LocalDateTime.of(2026, 10, 26, 15, 48, 30);
        private BigDecimal payableAmount = new BigDecimal("7999.00");

        public String getOrderNo() {
            return orderNo;
        }

        public String getCustomerPhone() {
            return customerPhone;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public BigDecimal getPayableAmount() {
            return payableAmount;
        }
    }

    @Test
    @DisplayName("普通 String 字段保持原值，不再被脱敏序列化器吞成 null")
    void keepsPlainStrings() throws Exception {
        String json = objectMapper.writeValueAsString(new OrderPayload());

        assertThat(json).contains("\"orderNo\":\"ORD-2026-001\"");
    }

    @Test
    @DisplayName("@Sensitive 字段仍然脱敏")
    void stillMasksAnnotatedStrings() throws Exception {
        String json = objectMapper.writeValueAsString(new OrderPayload());

        assertThat(json).contains("\"customerPhone\":\"138****8001\"");
    }

    @Test
    @DisplayName("Map / List 里的 String 也不丢失")
    void keepsStringsOutsideBeans() throws Exception {
        assertThat(objectMapper.writeValueAsString(Map.of("skuCode", "SKU-001-128")))
            .isEqualTo("{\"skuCode\":\"SKU-001-128\"}");
        assertThat(objectMapper.writeValueAsString(List.of("a", "b"))).isEqualTo("[\"a\",\"b\"]");
        assertThat(objectMapper.writeValueAsString("bare")).isEqualTo("\"bare\"");
    }

    @Test
    @DisplayName("LocalDateTime 序列化为 ISO-8601 字符串而非数字数组")
    void writesIsoDates() throws Exception {
        String json = objectMapper.writeValueAsString(new OrderPayload());

        assertThat(json).contains("\"createTime\":\"2026-10-26T15:48:30\"");
        assertThat(json).doesNotContain("[2026,10,26");
    }

    @Test
    @DisplayName("序列化整个 POJO 不再触发递归栈溢出")
    void doesNotOverflowStack() {
        assertThatCode(() -> objectMapper.writeValueAsString(new OrderPayload()))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("未知字段不导致请求体反序列化失败")
    void ignoresUnknownProperties() {
        assertThatCode(() -> objectMapper.readValue(
            "{\"orderNo\":\"ORD-1\",\"uiOnlyField\":true}", OrderPayload.class))
            .doesNotThrowAnyException();
    }
}
