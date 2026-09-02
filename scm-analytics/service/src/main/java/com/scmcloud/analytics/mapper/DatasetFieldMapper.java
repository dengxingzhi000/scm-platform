package com.scmcloud.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DatasetFieldMapper extends BaseMapper<DatasetFieldDefinition> {

    @Select("""
            SELECT * FROM analytics_dataset_field
            WHERE tenant_id = #{tenantId}::uuid
              AND dataset_id = #{datasetId}::uuid
              AND NOT deleted
            ORDER BY sort_order ASC, field_code ASC
            """)
    List<DatasetFieldDefinition> selectByDataset(@Param("tenantId") String tenantId,
                                                 @Param("datasetId") UUID datasetId);

    @Select("""
            SELECT * FROM analytics_dataset_field
            WHERE dataset_id = #{datasetId}::uuid
              AND field_code = #{code}
              AND NOT deleted
            """)
    DatasetFieldDefinition selectByDatasetAndCode(@Param("datasetId") UUID datasetId,
                                                  @Param("code") String code);

    @Select("SELECT COUNT(*) FROM analytics_dataset_field WHERE id = #{id} AND NOT deleted")
    long countById(@Param("id") UUID id);
}