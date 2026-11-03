package com.cognora.platform.gateway.config;

import com.cognora.platform.gateway.properties.IpAccessControlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit configuration hook for IP control properties binding.
 */
@Configuration
@EnableConfigurationProperties(IpAccessControlProperties.class)
public class IpAccessControlConfiguration { }
