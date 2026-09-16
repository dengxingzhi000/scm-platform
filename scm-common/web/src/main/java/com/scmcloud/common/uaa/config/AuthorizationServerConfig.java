package com.scmcloud.common.uaa.config;

import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.web.domain.SecurityUser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 Authorization Server Configuration
 * 
 * Migrated to Spring Security 7.0 API:
 * - Uses http.oauth2AuthorizationServer() instead of OAuth2AuthorizationServerConfigurer
 * - Uses @Import(OAuth2AuthorizationServerConfiguration.class) for default configuration
 *
 * @author Deng
 * @version 2.0
 */
@Configuration
@RequiredArgsConstructor
@Import(OAuth2AuthorizationServerConfiguration.class)
public class AuthorizationServerConfig {
    @Value("${security.oauth2.authorizationserver.issuer:http://localhost:8090}")
    private String issuer;

    // Optional keystore-based JWK configuration (fallback to generated if missing)
    @Value("${security.oauth2.authorizationserver.jwk.keystore-location:}")
    private String keystoreLocation;
    @Value("${security.oauth2.authorizationserver.jwk.keystore-password:}")
    private String keystorePassword;
    @Value("${security.oauth2.authorizationserver.jwk.key-alias:}")
    private String keyAlias;
    @Value("${security.oauth2.authorizationserver.jwk.key-password:}")
    private String keyPassword;

    /**
     * OAuth2鎺堟潈鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ら摼
     * Uses the new Spring Security 7.0 API with http.oauth2AuthorizationServer()
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .oauth2AuthorizationServer(authorizationServer ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                                .authorizationServerSettings(authorizationServerSettings())
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

        return http.build();
    }

    /**
     * 榛樿瀹夊叏杩囨护锟?
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**", "/login", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * 娉ㄥ唽瀹㈡埛锟?
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // Web 瀹㈡埛锟?
        RegisteredClient webClient = RegisteredClient.withId(UUIDv7Util.generateString())
                .clientId("nearsync-web")
                .clientSecret(passwordEncoder().encode("web-secret-2024"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://localhost:3000/callback")
                .redirectUri("http://localhost:3000/authorized")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("user.read")
                .scope("user.write")
                .scope("system.admin")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .build();

        // 绉诲姩瀹㈡埛绔紙浣跨敤PKCE锟?
        RegisteredClient mobileClient = RegisteredClient.withId(UUIDv7Util.generateString())
                .clientId("nearsync-mobile")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // 鍏紑瀹㈡埛锟?
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("nearsync://callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("user.read")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(true) // 寮哄埗 PKCE
                        .build())
                .build();

        // 鏈嶅姟闂磋皟鐢ㄥ鎴风
        RegisteredClient serviceClient = RegisteredClient.withId(UUIDv7Util.generateString())
                .clientId("internal-service")
                .clientSecret(passwordEncoder().encode("service-secret-2024"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("service.internal")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(webClient, mobileClient, serviceClient);
    }

    /**
     * JWT 瑙g爜锟?
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * JWK婧愶紙浣跨敤RSA瀵嗛挜瀵癸級
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // 浼樺厛锟絢eystore 鍔犺浇锛涘け璐ュ垯鍥為€€鍒板惎鍔ㄦ椂鐢熸垚
        RSAKey rsaKey = loadRsaFromKeystore();
        if (rsaKey == null) {
            KeyPair keyPair = generateRsaKey();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        }

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * 鐢熸垚 RSA瀵嗛挜锟?
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    /**
     * 鎺堟潈鏈嶅姟鍣ㄨ锟?
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (!"access_token".equals(context.getTokenType().getValue())) {
                return;
            }

            AuthorizationGrantType grantType = context.getAuthorizationGrantType();
            boolean userGrant = AuthorizationGrantType.AUTHORIZATION_CODE.equals(grantType)
                    || AuthorizationGrantType.REFRESH_TOKEN.equals(grantType)
                    || new AuthorizationGrantType("password").equals(grantType);

            if (!userGrant) {
                return;
            }

            Authentication principal = context.getPrincipal();
            if (principal != null && principal.getPrincipal() instanceof SecurityUser user) {
                context.getClaims().claims(claims -> {
                    claims.put("userId", String.valueOf(user.getUserId()));
                    claims.put("deptId", String.valueOf(user.getDeptId()));
                    claims.put("roles", user.getRoles());
                    claims.put("permissions", user.getPermissions());
                });
            }
        };
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    private RSAKey loadRsaFromKeystore() {
        if (!hasText(keystoreLocation) || !hasText(keyAlias)) {
            return null;
        }
        try {
            var resource = new DefaultResourceLoader().getResource(keystoreLocation);
            if (!resource.exists()) {
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                KeyStore keyStore = KeyStore.getInstance("JKS");
                char[] ksPass = hasText(keystorePassword) ? keystorePassword.toCharArray() : null;
                keyStore.load(is, ksPass);

                char[] keyPass = hasText(keyPassword) ? keyPassword.toCharArray() : null;
                Key key = keyStore.getKey(keyAlias, keyPass);
                if (key instanceof RSAPrivateKey privateKey) {
                    var cert = keyStore.getCertificate(keyAlias);
                    RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
                    return new RSAKey.Builder(publicKey)
                            .privateKey(privateKey)
                            .keyID(UUID.randomUUID().toString())
                            .build();
                }
            }
        } catch (Exception ignored) {
            // ignore and fallback
        }
        return null;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
