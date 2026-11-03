package com.scmcloud.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.analytics.domain.entity.LineageEdge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LineageEdgeMapper extends BaseMapper<LineageEdge> {

    @Select("""
            SELECT * FROM analytics_lineage_edge
            WHERE tenant_id = #{tenantId}::uuid
              AND NOT deleted
            ORDER BY upstream_node ASC, downstream_node ASC
            """)
    List<LineageEdge> selectByTenant(@Param("tenantId") String tenantId);

    @Select("""
            SELECT * FROM analytics_lineage_edge
            WHERE tenant_id = #{tenantId}::uuid
              AND downstream_node = #{node}
              AND NOT deleted
            ORDER BY upstream_node ASC
            """)
    List<LineageEdge> selectUpstream(@Param("tenantId") String tenantId, @Param("node") String node);

    @Select("""
            SELECT * FROM analytics_lineage_edge
            WHERE tenant_id = #{tenantId}::uuid
              AND upstream_node = #{node}
              AND NOT deleted
            ORDER BY downstream_node ASC
            """)
    List<LineageEdge> selectDownstream(@Param("tenantId") String tenantId, @Param("node") String node);
}
