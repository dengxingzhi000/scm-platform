package com.scmcloud.auth.saml;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * SAML2配置。
 *
 * <p>支持企业级SSO集成：
 * <ul>
 *   <li>Okta</li>
 *   <li>Azure AD</li>
 *   <li>OneLogin</li>
 *   <li>自建IdP</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "saml", name = "enabled", havingValue = "true")
public class SamlConfig {

    private final SamlProperties samlProperties;

    /**
     * 配置Relying Party Registration Repository。
     */
    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() throws IOException {
        log.info("配置SAML Relying Party: idpMetadataUrl={}", samlProperties.getIdpMetadataUrl());

        // 从IdP metadata URL加载元数据
        InputStream metadataStream = new URL(samlProperties.getIdpMetadataUrl()).openStream();

        RelyingPartyRegistration registration = RelyingPartyRegistration
                .withMetadata(metadataStream)
                .registrationId(samlProperties.getTenantId())
                .entityId(samlProperties.getSpEntityId())
                .assertionConsumerServiceLocation(samlProperties.getSpAcsUrl())
                .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    /**
     * 创建SAML认证提供者。
     */
    @Bean
    public OpenSaml4AuthenticationProvider authenticationProvider() {
        return new OpenSaml4AuthenticationProvider();
    }

    /**
     * 创建SAML元数据过滤器。
     */
    @Bean
    public Saml2MetadataFilter saml2MetadataFilter(
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        Saml2MetadataFilter filter = new Saml2MetadataFilter(
                new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository),
                new OpenSamlMetadataResolver());
        filter.setRequestMatcher(
                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antPath("/saml/metadata"));
        return filter;
    }
}
