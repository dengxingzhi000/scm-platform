package com.scmcloud.approval.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable BPMN引擎配置。
 *
 * <p>配置要点：
 * <ul>
 *   <li>使用Spring事务管理器</li>
 *   <li>启用异步执行</li>
 *   <li>设置数据库策略为自动更新</li>
 * </ul>
 */
@Configuration
public class FlowableConfig {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> engineConfigurer() {
        return configuration -> {
            configuration.setActivityFontName("宋体");
            configuration.setLabelFontName("宋体");
            configuration.setAnnotationFontName("宋体");
        };
    }
}
