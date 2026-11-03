package com.scmcloud.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Shared OpenAPI 3 configuration for every MVC service.
 *
 * <p>Registered through {@code AutoConfiguration.imports} so that services which do not
 * component-scan {@code com.scmcloud.common} still expose the same document metadata.
 * {@link OpenApiPropertiesEnvironmentPostProcessor} exposes the generated document at
 * {@code /api/public/v3/api-docs}; the interactive Swagger UI is disabled by default.</p>
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfig {

    /**
     * Security scheme name as it appears in {@code components.securitySchemes}.
     * <p>The OpenAPI specification restricts this key to {@code ^[a-zA-Z0-9.\-_]+$},
     * so it must not contain spaces — openapi-generator rejects the document otherwise.</p>
     */
    private static final String SECURITY_SCHEME_NAME = "BearerAuthentication";

    @Value("${spring.application.name:scm-platform}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${scm.openapi.server-url:}")
    private String serverUrl;

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI scmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SCM Platform API - " + applicationName)
                        .version("1.0.0")
                        .description("Enterprise Supply Chain Management Platform API")
                        .contact(new Contact()
                                .name("SCM Team")
                                .email("admin@scmcloud.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(new Server()
                        .url(StringUtils.hasText(serverUrl) ? serverUrl : "http://localhost:" + serverPort)
                        .description("Local Development")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .description("JWT token, e.g. Bearer eyJhbGciOi...")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
