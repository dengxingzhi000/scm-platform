package com.scmcloud.common.rest.config;

import com.scmcloud.common.rest.client.SysAuthServiceClient;
import com.scmcloud.common.rest.client.SysAuditServiceClient;
import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import com.scmcloud.common.rest.client.SysUserServiceClient;
import com.scmcloud.common.rest.interceptor.RestClientRequestSignatureInterceptor;
import com.scmcloud.common.security.util.AuthTokenContext;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.UUID;

/**
 * RestClient + HttpExchange 鏍稿績閰嶇疆
 * <p>鏇夸唬 OpenFeign 閰嶇疆</p>
 *
 * <p>鍔熻兘锟?
 * <ul>
 *   <li>鍒涘缓 RestClient Bean锛堝寘锟絤TLS + 绛惧悕鎷︽埅鍣級</li>
 *   <li>鍒涘缓 HttpServiceProxyFactory锛堢敤锟絳@code @HttpExchange} 浠g悊锟?li>
 *   <li>闆嗘垚 Nacos 鏈嶅姟鍙戠幇锛堝姩鎬佽В鏋愭湇鍔″湴鍧€锟?li>
 *   <li>娉ㄥ唽 3 涓鎴风 Bean锛圲serServiceClient銆丄uthServiceClient銆丳ermissionServiceClient锟?li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@NullMarked
public class RestClientHttpExchangeConfig {
    /**
     * Service IDs consumed by the load-balanced RestClient. Spring Cloud LoadBalancer resolves
     * them against the current instance list on every request, so a restart or IP change does
     * not require rebuilding the client.
     */
    private static final String SYSTEM_SERVICE = "http://system-service";
    private static final String AUTH_SERVICE = "http://auth-service";
    private static final String AUDIT_SERVICE = "http://scm-audit";

    private final ClientHttpRequestFactory clientHttpRequestFactory;
    private final RestClientRequestSignatureInterceptor signatureInterceptor;

    /**
     * 鍒涘缓鍩虹 RestClient Bean
     * <p>鍖呭惈 mTLS + 绛惧悕鎷︽埅锟?p>
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .requestFactory(clientHttpRequestFactory)
            .requestInterceptor(signatureInterceptor)
            .requestInterceptor((request, body, execution) -> {
                // Tenant is forwarded even without a token: audit and other internal writes may
                // happen before/after authentication but still need the header.
                UUID tenantId = TenantContextHolder.getTenantId();
                if (tenantId != null) {
                    request.getHeaders().set("X-Tenant-Id", tenantId.toString());
                }
                String token = AuthTokenContext.getToken();
                if (token != null && request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null) {
                    request.getHeaders().setBearerAuth(token);
                    String deviceId = AuthTokenContext.getDeviceId();
                    if (deviceId != null) {
                        request.getHeaders().set("X-Device-ID", deviceId);
                    }
                }
                return execution.execute(request, body);
            });
    }

    /**
     * 鍒涘缓 SysUserServiceClient Bean
     */
    @Bean
    @Lazy
    public SysUserServiceClient sysUserServiceClient(RestClient.Builder restClientBuilder) {
        // Users live in scm-system (db_user), which registers as "system-service". The
        // load-balanced RestClient resolves the service ID on every call.
        String baseUrl = SYSTEM_SERVICE;
        log.info("Creating SysUserServiceClient with load-balanced baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder.clone()
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysUserServiceClient.class);
    }

    /**
     * 鍒涘缓 SysAuthServiceClient Bean
     */
    @Bean
    @Lazy
    public SysAuthServiceClient sysAuthServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = AUTH_SERVICE;
        log.info("Creating SysAuthServiceClient with load-balanced baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder.clone()
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysAuthServiceClient.class);
    }

    /**
     * 鍒涘缓 SysPermissionServiceClient Bean
     */
    @Bean
    @Lazy
    public SysPermissionServiceClient sysPermissionServiceClient(RestClient.Builder restClientBuilder) {
        // /api/system/permissions/** is served by scm-system ("system-service"), not a standalone
        // permission service; ApiAccessControlFilter fails closed, so a wrong name denied every API.
        String baseUrl = SYSTEM_SERVICE;
        log.info("Creating SysPermissionServiceClient with load-balanced baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder.clone()
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysPermissionServiceClient.class);
    }

    /**
     * Client for scm-audit's record endpoint ({@code POST /sys-audit-log}).
     */
    @Bean
    @Lazy
    public SysAuditServiceClient sysAuditServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = AUDIT_SERVICE;
        log.info("Creating SysAuditServiceClient with load-balanced baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder.clone()
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysAuditServiceClient.class);
    }

}
