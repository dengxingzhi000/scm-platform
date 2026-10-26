package com.scmcloud.common.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Publishes the OpenAPI document under the already public {@code /api/public} namespace so
 * Swagger UI's webjars do not need additional security exemptions.
 */
public class OpenApiPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "scmOpenApiDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("springdoc.api-docs.path", "/api/public/v3/api-docs");
        defaults.put("springdoc.swagger-ui.enabled", false);

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
}
