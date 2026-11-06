package com.cognora.platform.common.rest.config;

import com.cognora.platform.common.rest.interceptor.RestClientRequestSignatureInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 审计上报地址必须可配置：老环境把审计服务注册为 {@code scm-audit}，新环境是
 * {@code platform-audit}；写死会让遥测上报在错误的环境里全部失败且被吞掉。
 */
class AuditServiceClientConfigTest {

    @Test
    void auditServiceIdDefaultsToTheCurrentRegistrationName() throws Exception {
        Field field = RestClientHttpExchangeConfig.class.getDeclaredField("auditServiceId");
        Value value = field.getAnnotation(Value.class);

        assertThat(value).isNotNull();
        assertThat(value.value()).isEqualTo("${cognora.audit.service-id:platform-audit}");
    }

    @Test
    void auditServiceUrlFollowsTheConfiguredServiceId() {
        RestClientHttpExchangeConfig config = new RestClientHttpExchangeConfig(
                mock(ClientHttpRequestFactory.class), mock(RestClientRequestSignatureInterceptor.class));
        ReflectionTestUtils.setField(config, "auditServiceId", "scm-audit");

        assertThat(config.auditServiceUrl()).isEqualTo("http://scm-audit");
    }

    @Test
    void auditServiceUrlKeepsTheDefaultWhenNotOverridden() {
        RestClientHttpExchangeConfig config = new RestClientHttpExchangeConfig(
                mock(ClientHttpRequestFactory.class), mock(RestClientRequestSignatureInterceptor.class));
        ReflectionTestUtils.setField(config, "auditServiceId", "platform-audit");

        assertThat(config.auditServiceUrl()).isEqualTo("http://platform-audit");
    }
}
