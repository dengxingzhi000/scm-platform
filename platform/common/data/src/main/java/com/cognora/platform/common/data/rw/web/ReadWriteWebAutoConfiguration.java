package com.cognora.platform.common.data.rw.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Registers the request-scope filter that drives read-write routing and cleans the
 * routing context at the end of every servlet request.
 *
 * @author Deng
 * @since 2025-12-16
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
@ConditionalOnProperty(
        prefix = "spring.datasource.rw",
        name = "enabled",
        havingValue = "true"
)
public class ReadWriteWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReadWriteContextFilter readWriteContextFilter() {
        return new ReadWriteContextFilter();
    }
}
