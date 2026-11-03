package com.scmcloud.common.security.config;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTPS Redirect Configuration
 * 
 * Migrated to Spring Boot 4.0 API:
 * - Uses addConnectorCustomizers() instead of addAdditionalTomcatConnectors()
 *
 * @author Deng
 * @version 2.0
 */
@Configuration
@RequiredArgsConstructor
public class HttpsRedirectConfig {
    private final HttpsRedirectProperties properties;

    @Bean
    public ServletWebServerFactory servletContainer() {
        if (!properties.isEnabled()) {
            return new TomcatServletWebServerFactory();
        }
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };

        // In Spring Boot 4.0, addAdditionalTomcatConnectors() was removed
        // Use addConnectorCustomizers() instead
        tomcat.addConnectorCustomizers(connector -> {
            connector.setScheme("http");
            connector.setPort(properties.getHttpPort());
            connector.setSecure(false);
            connector.setRedirectPort(properties.getRedirectPort());
        });

        return tomcat;
    }
}
