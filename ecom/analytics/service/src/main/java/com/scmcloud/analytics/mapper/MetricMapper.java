package com.scmcloud.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.UUID;

@Mapper
public interface MetricMapper extends BaseMapper<MetricDefinition> {

    @Select("""
            SELECT * FROM analytics_metric
            WHERE tenant_id = #{tenantId}::uuid
              AND metric_code = #{code}
              AND NOT deleted
            """)
    MetricDefinition selectByTenantAndCode(@Param("tenantId") String tenantId,
                                          @Param("code") String code);

    @Select("""
            SELECT * FROM analytics_metric
            WHERE tenant_id = #{tenantId}::uuid
              AND NOT deleted
              AND status = 1
            ORDER BY sort_order ASC, metric_code ASC
            """)
    java.util.List<MetricDefinition> selectVisibleByTenant(@Param("tenantId") String tenantId);

    @Select("SELECT COUNT(*) FROM analytics_metric WHERE id = #{id} AND NOT deleted")
    long countById(@Param("id") UUID id);
}