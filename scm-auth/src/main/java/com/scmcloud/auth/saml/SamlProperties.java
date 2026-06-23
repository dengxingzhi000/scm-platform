package com.scmcloud.auth.saml;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SAML配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "saml")
public class SamlProperties {

    /**
     * 是否启用SAML。
     */
    private boolean enabled = false;

    /**
     * Identity Provider (IdP) metadata URL。
     */
    private String idpMetadataUrl;

    /**
     * Service Provider (SP) entity ID。
     */
    private String spEntityId;

    /**
     * SP ACS URL (Assertion Consumer Service)。
     */
    private String spAcsUrl;

    /**
     * SP SLO URL (Single Logout Service)。
     */
    private String spSloUrl;

    /**
     * 租户ID（多租户SAML配置）。
     */
    private String tenantId;
}
