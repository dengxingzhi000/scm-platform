package com.scmcloud.common.mybatisPlus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.Quantity;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.common.mybatisPlus.handler.MoneyTypeHandler;
import com.scmcloud.common.mybatisPlus.handler.QuantityTypeHandler;
import com.scmcloud.common.mybatisPlus.handler.TenantIdTypeHandler;
import com.scmcloud.common.mybatisPlus.handler.UUIDTypeHandler;
import com.scmcloud.common.mybatisPlus.properties.MybatisPlusProperties;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * MybatisPlus 閰嶇疆锟?
 *
 * @author Deng
 * createData 2025/10/15 13:35
 * @version 1.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 娣诲姞鍒嗛〉鎻掍欢
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 鍒嗛〉鎻掍欢
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(properties.getPaginationMaxLimit()); // 鏈€澶у崟椤甸檺鍒舵暟锟?
        paginationInterceptor.setOverflow(false); // 婧㈠嚭鎬婚〉鏁板悗鏄惁杩涜澶勭悊
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 涔愯閿佹彃锟?
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 闃叉鍏ㄨ〃鏇存柊涓庡垹闄ゆ彃锟?
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
    
    /**
     * 娉ㄥ唽鑷畾涔夌被鍨嬪鐞嗗櫒
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            // 娉ㄥ唽 UUID绫诲瀷澶勭悊锟?
            typeHandlerRegistry.register(UUID.class, UUIDTypeHandler.class);
            // 娉ㄥ唽鍊煎璞＄被鍨嬪鐞嗗櫒
            typeHandlerRegistry.register(Money.class, MoneyTypeHandler.class);
            typeHandlerRegistry.register(Quantity.class, QuantityTypeHandler.class);
            typeHandlerRegistry.register(TenantId.class, TenantIdTypeHandler.class);
        };
    }

    @Bean
    public MybatisPlusProperties mybatisPlusProperties() {
        return new MybatisPlusProperties();
    }
}
