package com.scmcloud.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.analytics.domain.entity.DataQualityResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DataQualityResultMapper extends BaseMapper<DataQualityResult> {

    @Select("""
            SELECT * FROM analytics_quality_result
            WHERE tenant_id = #{tenantId}::uuid
              AND rule_id = #{ruleId}::uuid
              AND NOT deleted
            ORDER BY check_date DESC, create_time DESC
            LIMIT 100
            """)
    List<DataQualityResult> selectByRule(@Param("tenantId") String tenantId,
                                         @Param("ruleId") UUID ruleId);

    @Select("""
            SELECT * FROM analytics_quality_result
            WHERE tenant_id = #{tenantId}::uuid
              AND rule_id = #{ruleId}::uuid
              AND check_date = #{checkDate}::date
              AND NOT deleted
            ORDER BY create_time DESC
            LIMIT 100
            """)
    List<DataQualityResult> selectByRuleAndDate(@Param("tenantId") String tenantId,
                                                @Param("ruleId") UUID ruleId,
                                                @Param("checkDate") LocalDate checkDate);
}
