package com.scmcloud.common.mybatisPlus.config;

import com.scmcloud.common.mybatisPlus.service.DataPermissionService;
import com.scmcloud.common.mybatisPlus.service.DefaultDataPermissionService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
public class DataPermissionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataPermissionService.class)
    public DefaultDataPermissionService defaultDataPermissionService(JdbcTemplate jdbcTemplate) {
        return new DefaultDataPermissionService(jdbcTemplate);
    }
}
