package com.scmcloud.common.feature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FeatureFlagService {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

    private final boolean defaultFlag;

    /**
     * {@code @Value} cannot bind a nested {@code feature-flags.*} YAML map, so the flags are read
     * through {@link Binder}; an absent section yields an empty map instead of failing startup.
     */
    public FeatureFlagService(Environment environment) {
        this.defaultFlag = environment.getProperty("feature-flags.default", boolean.class, false);
        Map<String, Boolean> configFlags = Binder.get(environment)
                .bind("feature-flags", Bindable.mapOf(String.class, Boolean.class))
                .orElseGet(Map::of);
        this.flags.putAll(configFlags);
        log.info("Feature flags initialized: {}", flags);
    }

    public boolean isEnabled(String flagName) {
        return flags.getOrDefault(flagName, defaultFlag);
    }

    public void setFlag(String flagName, boolean enabled) {
        flags.put(flagName, enabled);
    }
}
