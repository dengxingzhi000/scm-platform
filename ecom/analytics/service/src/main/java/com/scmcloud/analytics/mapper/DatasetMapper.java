package com.scmcloud.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.UUID;

@Mapper
public interface DatasetMapper extends BaseMapper<DatasetDefinition> {

    @Select("""
            SELECT * FROM analytics_dataset
            WHERE tenant_id = #{tenantId}::uuid
              AND dataset_code = #{code}
              AND NOT deleted
            """)
    DatasetDefinition selectByTenantAndCode(@Param("tenantId") String tenantId,
                                            @Param("code") String code);

    @Select("""
            SELECT * FROM analytics_dataset
            WHERE tenant_id = #{tenantId}::uuid
              AND source_type = #{sourceType}
              AND NOT deleted
              AND enabled = TRUE
            ORDER BY dataset_code
            """)
    java.util.List<DatasetDefinition> selectBySourceType(@Param("tenantId") String tenantId,
                                                         @Param("sourceType") String sourceType);

    @Select("SELECT COUNT(*) FROM analytics_dataset WHERE id = #{id} AND NOT deleted")
    long countById(@Param("id") UUID id);
}