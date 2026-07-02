package com.scmcloud.common.rest.config;

import com.scmcloud.common.rest.client.SysAuthServiceClient;
import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import com.scmcloud.common.rest.client.SysUserServiceClient;
import com.scmcloud.common.rest.interceptor.RestClientRequestSignatureInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.URI;

/**
 * RestClient + HttpExchange 鏍稿績閰嶇疆
 * <p>鏇夸唬 OpenFeign 閰嶇疆</p>
 *
 * <p>鍔熻兘锟?
 * <ul>
 *   <li>鍒涘缓 RestClient Bean锛堝寘锟絤TLS + 绛惧悕鎷︽埅鍣級</li>
 *   <li>鍒涘缓 HttpServiceProxyFactory锛堢敤锟絳@code @HttpExchange} 浠ｇ悊锟?li>
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
    private final ClientHttpRequestFactory clientHttpRequestFactory;
    private final RestClientRequestSignatureInterceptor signatureInterceptor;
    private final LoadBalancerClient loadBalancerClient;

    /**
     * 鍒涘缓鍩虹 RestClient Bean
     * <p>鍖呭惈 mTLS + 绛惧悕鎷︽埅锟?p>
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .requestFactory(clientHttpRequestFactory)
            .requestInterceptor(signatureInterceptor);
    }

    /**
     * 鍒涘缓 SysUserServiceClient Bean
     */
    @Bean
    public SysUserServiceClient sysUserServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = resolveServiceUrl("user-service");
        log.info("Creating SysUserServiceClient with baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder
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
    public SysAuthServiceClient sysAuthServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = resolveServiceUrl("auth-service");
        log.info("Creating SysAuthServiceClient with baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder
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
    public SysPermissionServiceClient sysPermissionServiceClient(RestClient.Builder restClientBuilder) {
        String baseUrl = resolveServiceUrl("permission-service");
        log.info("Creating SysPermissionServiceClient with baseUrl: {}", baseUrl);

        RestClient restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(SysPermissionServiceClient.class);
    }

    /**
     * 锟絅acos 鏈嶅姟鍙戠幇瑙ｆ瀽鏈嶅姟 URL锛堟敮鎸佽礋杞藉潎琛★級
     *
     * <p>瑙ｆ瀽绛栫暐锟?
     * <ol>
     *   <li>浣跨敤 Spring Cloud LoadBalancer 閫夋嫨鏈嶅姟瀹炰緥</li>
     *   <li>鏀寔澶氱璐熻浇鍧囪　绛栫暐锛圧ound Robin銆丷andom銆乄eighted 绛夛級</li>
     *   <li>鏍规嵁瀹炰緥锟絪ecure 鏍囧織閫夋嫨 https/http</li>
     *   <li>濡傛灉瑙ｆ瀽澶辫触锛屼娇鐢ㄩ粯锟経RL锛坽@code http://serviceName}锟?li>
     * </ol>
     *
     * <p>璐熻浇鍧囪　绛栫暐锟絊pring Cloud LoadBalancer 閰嶇疆鍐冲畾锛岄粯璁や负 Round Robin锟?
     * 鍙€氳繃閰嶇疆绫昏嚜瀹氫箟绛栫暐锛堝 RandomLoadBalancer銆乄eightedServiceInstanceListSupplier 绛夛級锟?
     *
     * @param serviceName 鏈嶅姟鍚嶇О锛堝 "user-service"锟?
     * @return 鏈嶅姟鍩虹 URL锛堝 {@code https://192.168.1.100:8081}锟?
     */
    private String resolveServiceUrl(String serviceName) {
        try {
            // 浣跨敤 LoadBalancerClient 閫夋嫨瀹炰緥锛堣嚜鍔ㄨ礋杞藉潎琛★級
            // Note: 铏界劧褰撳墠鐗堟湰涓嶈繑锟絥ull锛屼絾淇濈暀妫€鏌ヤ綔涓洪槻寰℃€х紪锟?
            ServiceInstance instance = loadBalancerClient.choose(serviceName);

            if (instance == null) {
                log.warn("No available instances for service '{}', using default URL", serviceName);
                return "http://" + serviceName;
            }

            URI uri = instance.getUri();
            log.debug("Resolved service '{}' to URL: {} (host: {}, metadata: {})",
                     serviceName, uri, instance.getHost(), instance.getMetadata());
            return uri.toString();

        } catch (Exception e) {
            log.error("Failed to resolve service '{}' via LoadBalancer: {}, using default URL",
                      serviceName, e.getMessage());
            return "http://" + serviceName;
        }
    }
}
