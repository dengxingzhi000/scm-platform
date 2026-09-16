package com.scmcloud.common.rest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Cloud LoadBalancer 璐熻浇鍧囪 绛栫暐閰嶇疆
 *
 * <p>鏀寔鐨勮礋杞藉潎琛$瓥鐣ワ細
 * <ul>
 *   <li><b>round-robin</b>锛堥粯璁わ級: 杞绛栫暐锛屼緷娆¢€夋嫨瀹炰緥</li>
 *   <li><b>random</b>: 闅忔満绛栫暐锛岄殢鏈洪€夋嫨瀹炰緥</li>
 *   <li><b>weighted-round-robin</b>: 鍔犳潈杞锛屾牴鎹疄渚嬫潈閲嶅垎閰嶈锟?li>
 * </ul>
 *
 * <p>閰嶇疆鏂瑰紡锛坅pplication.yml锟?
 * <pre>
 * spring:
 *   cloud:
 *     loadbalancer:
 *       # 璐熻浇鍧囪 绛栫暐: round-robin锛堥粯璁わ級, random, weighted-round-robin
 *       strategy: round-robin
 *
 *       # Nacos 鏉冮噸閰嶇疆锛堜粎 weighted-round-robin 绛栫暐鐢熸晥锟?
 *       nacos:
 *         enabled: true
 * </pre>
 *
 * <p>Nacos 瀹炰緥鏉冮噸閰嶇疆绀轰緥锟?
 * <pre>
 * # 锟絅acos 鎺у埗鍙伴厤缃疄锟絤etadata
 * weight: 80  # 鏉冮噸锟?-100锛岄粯锟?00
 * </pre>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
public class LoadBalancerConfiguration {

    /**
     * Round Robin 璐熻浇鍧囪 鍣紙榛樿绛栫暐锟?
     * <p>杞閫夋嫨瀹炰緥锛岄€傜敤浜庡疄渚嬫€ц兘鐩歌繎鐨勫満锟?p>
     */
    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "round-robin",
        matchIfMissing = true
    )
    public ReactorLoadBalancer<ServiceInstance> roundRobinLoadBalancer(
            Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using RoundRobinLoadBalancer for service: {}", name);

        return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, name);
    }

    /**
     * Random 璐熻浇鍧囪 锟?
     * <p>闅忔満閫夋嫨瀹炰緥锛岄€傜敤浜庡揩閫熷垎鏁h姹傜殑鍦烘櫙</p>
     */
    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "random"
    )
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using RandomLoadBalancer for service: {}", name);

        return new RandomLoadBalancer(serviceInstanceListSupplierProvider, name);
    }

    /**
     * Weighted Round Robin 璐熻浇鍧囪 鍣紙鍩轰簬 Nacos 鏉冮噸锟?
     * <p>鏍规嵁瀹炰緥鏉冮噸鍒嗛厤璇锋眰锛岄€傜敤浜庡疄渚嬫€ц兘宸紓杈冨ぇ鐨勫満锟?p>
     *
     * <p>鏉冮噸閰嶇疆锟?
     * <ul>
     *   <li>锟絅acos 鎺у埗鍙拌缃疄锟絤etadata: {@code weight=80}</li>
     *   <li>鏉冮噸鑼冨洿: 0-100锛岄粯锟?00</li>
     *   <li>鏉冮噸锟? 鐨勫疄渚嬩笉鎺ユ敹璇锋眰</li>
     * </ul>
     *
     * <p>娉ㄦ剰锛歋pring Cloud LoadBalancer 2023.0.0+ 鍘熺敓鏀寔 Nacos 鏉冮噸锟?
     * 鍙渶閰嶇疆 {@code spring.cloud.loadbalancer.nacos.enabled=true} 鍗冲彲锟?
     * 锟紹ean 浣跨敤 RoundRobinLoadBalancer 閰嶅悎 Nacos 鏉冮噸鐗规€у疄鐜帮拷
     */
    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "weighted-round-robin"
    )
    public ReactorLoadBalancer<ServiceInstance> weightedRoundRobinLoadBalancer(
            Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using WeightedRoundRobinLoadBalancer (Nacos-based) for service: {}", name);

        // Spring Cloud Alibaba 锟絅acosServiceInstanceListSupplier 浼氳嚜锟?
        // 鏍规嵁 Nacos 瀹炰緥锟絯eight 瀛楁杩囨护鍜屽姞鏉冨疄渚嬪垪锟?
        return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, name);
    }
}
