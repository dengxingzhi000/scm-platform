package com.scmcloud.common.rest.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * RestClient mTLS 閰嶇疆 - 浣跨敤 Spring Boot 3.1+ SSL Bundles
 * 鏇夸唬 OpenFeign 锟紽eignMtlsConfig
 *
 * <p>鐗规€э細
 * <ul>
 *   <li>鍩轰簬 Spring Boot 4.0+ SSL Bundle 鏈哄埗</li>
 *   <li>鏀寔 mTLS 鍙屽悜璁よ瘉</li>
 *   <li>鏀寔璇佷功鏈夋晥鏈熺洃鎺у拰鍛婅</li>
 *   <li>闆跺仠鏈鸿瘉涔︾儹鏇存柊锛堥€氳繃 CertificateReloaderRestClient锟?li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Configuration
@Getter
public class RestClientMtlsConfig {

    @Value("${security.mtls.keystore-path:classpath:certificates/keystore.p12}")
    private Resource keystoreResource;

    @Value("${security.mtls.keystore-password:${KEYSTORE_PASSWORD:changeit}}")
    private String keystorePassword;

    @Value("${security.mtls.truststore-path:classpath:certificates/truststore.p12}")
    private Resource truststoreResource;

    @Value("${security.mtls.truststore-password:${TRUSTSTORE_PASSWORD:changeit}}")
    private String truststorePassword;

    @Value("${security.feign.app-id:${API_SECRET_INTERNAL_SERVICE:internal-service}}")
    private String appId;

    @Value("${security.feign.secret-key:${API_SECRET_INTERNAL_SECRET:your-internal-secret-key}}")
    private String secretKey;

    @Value("${spring.http.client.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${spring.http.client.read-timeout:30000}")
    private int readTimeout;

    /**
     * 閰嶇疆 SSL Bundle锛堟帹鑽愭柟寮忥級
     * <p>浣跨敤 Spring Boot 4.0 锟絊SL Bundle 鏈哄埗锛屾敮鎸佽瘉涔︾儹鏇存柊</p>
     */
    @Bean
    public SslBundle sslBundle() throws Exception {
        log.info("Initializing SSL Bundle for mTLS...");

        // 鍔犺浇 KeyStore 锟絋rustStore
        JksSslStoreDetails keystoreDetails = createStoreDetails(keystoreResource, keystorePassword);
        JksSslStoreDetails truststoreDetails = createStoreDetails(truststoreResource, truststorePassword);

        // 鍒涘缓 SSL Store Bundle
        JksSslStoreBundle storeBundle = new JksSslStoreBundle(keystoreDetails, truststoreDetails);

        // 鍒涘缓 SSL Bundle Key
        SslBundleKey key = SslBundleKey.of(keystorePassword, null);

        // 鏋勫缓 SSL Bundle
        SslBundle bundle = SslBundle.of(storeBundle, key);

        log.info("SSL Bundle initialized successfully");
        return bundle;
    }

    /**
     * 鍒涘缓 JKS Store Details
     */
    private JksSslStoreDetails createStoreDetails(Resource resource, String password) throws Exception {
        JksSslStoreDetails details = JksSslStoreDetails.forLocation(resource.getURL().toString());
        return new JksSslStoreDetails(
            details.type(),
            details.provider(),
            resource.getURL().toString(),
            password
        );
    }

    /**
     * 閰嶇疆 ClientHttpRequestFactory锛圧estClient 浣跨敤锟?
     * <p>浣跨敤 JDK HttpClient (Java 21+) 閰嶅悎 SSL Bundle</p>
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(SslBundle sslBundle) {
        log.info("Creating ClientHttpRequestFactory with mTLS support...");

        // 锟絊SL Bundle 鍒涘缓 SSLContext
        SSLContext sslContext = sslBundle.createSslContext();

        // 浣跨敤 JDK 21 锟紿ttpClient锛堟敮鎸佽櫄鎷熺嚎绋嬶級
        HttpClient httpClient = HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .build();

        // 鍒涘缓 JdkClientHttpRequestFactory
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        log.info("ClientHttpRequestFactory created: connectTimeout={}ms, readTimeout={}ms",
                 connectTimeout, readTimeout);
        return factory;
    }

    /**
     * 瀹氭椂妫€鏌ヨ瘉涔︽湁鏁堟湡锛堟瘡澶╁噷锟? 鐐癸級
     * <p>SECURITY: 鎻愬墠 30 澶╁憡璀﹁瘉涔﹀嵆灏嗚繃锟?p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkCertificateExpiry() {
        try {
            log.debug("Checking certificate expiry...");

            // 鍔犺浇 KeyStore
            KeyStore keyStore = loadKeyStore(keystoreResource, keystorePassword);

            // 妫€鏌ユ墍鏈夎瘉锟?
            var aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isCertificateEntry(alias) || keyStore.isKeyEntry(alias)) {
                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    if (cert != null) {
                        checkSingleCertificate(alias, cert);
                    }
                }
            }

            log.debug("Certificate expiry check completed");
        } catch (Exception e) {
            log.error("Failed to check certificate expiry: {}", e.getMessage(), e);
        }
    }

    /**
     * 妫€鏌ュ崟涓瘉涔︽湁鏁堟湡
     */
    private void checkSingleCertificate(String alias, X509Certificate cert) {
        Date notAfter = cert.getNotAfter();
        Instant expiryInstant = notAfter.toInstant();
        Instant now = Instant.now();
        Instant warningThreshold = now.plus(Duration.ofDays(30));

        if (expiryInstant.isBefore(now)) {
            log.error("SECURITY ALERT: Certificate '{}' has EXPIRED on {}", alias, notAfter);
        } else if (expiryInstant.isBefore(warningThreshold)) {
            long daysUntilExpiry = Duration.between(now, expiryInstant).toDays();
            log.warn("SECURITY WARNING: Certificate '{}' will expire in {} days ({})",
                     alias, daysUntilExpiry, notAfter);
        } else {
            long daysUntilExpiry = Duration.between(now, expiryInstant).toDays();
            log.info("Certificate '{}' is valid for {} more days (expires: {})",
                     alias, daysUntilExpiry, notAfter);
        }
    }

    /**
     * 鍔犺浇 KeyStore
     */
    private KeyStore loadKeyStore(Resource resource, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }
}
