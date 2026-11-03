package com.scmcloud.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.analytics.domain.entity.DataQualityRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DataQualityRuleMapper extends BaseMapper<DataQualityRule> {

    @Select("""
            SELECT * FROM analytics_quality_rule
            WHERE tenant_id = #{tenantId}::uuid
              AND enabled = TRUE
              AND status = 1
              AND NOT deleted
            ORDER BY severity DESC, rule_code ASC
            """)
    List<DataQualityRule> selectEnabledByTenant(@Param("tenantId") String tenantId);

    @Select("""
            SELECT DISTINCT tenant_id::text FROM analytics_quality_rule
            WHERE enabled = TRUE AND status = 1 AND NOT deleted
            """)
    List<String> selectDistinctTenantIds();
}
