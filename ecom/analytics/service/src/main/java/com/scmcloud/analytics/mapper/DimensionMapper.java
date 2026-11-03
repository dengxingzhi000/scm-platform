package com.scmcloud.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.analytics.domain.entity.DimensionDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.UUID;

@Mapper
public interface DimensionMapper extends BaseMapper<DimensionDefinition> {

    @Select("""
            SELECT * FROM analytics_dimension
            WHERE tenant_id = #{tenantId}::uuid
              AND dim_code = #{code}
              AND NOT deleted
            """)
    DimensionDefinition selectByTenantAndCode(@Param("tenantId") String tenantId,
                                              @Param("code") String code);

    @Select("""
            SELECT * FROM analytics_dimension
            WHERE tenant_id = #{tenantId}::uuid
              AND parent_dim_id = #{parentId}::uuid
              AND NOT deleted
            ORDER BY hierarchy_level, dim_code
            """)
    java.util.List<DimensionDefinition> selectByParent(@Param("tenantId") String tenantId,
                                                       @Param("parentId") UUID parentId);

    @Select("SELECT COUNT(*) FROM analytics_dimension WHERE id = #{id} AND NOT deleted")
    long countById(@Param("id") UUID id);
}