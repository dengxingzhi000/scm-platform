package com.cognora.platform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.file.api.dto.FileStatsDTO;
import com.cognora.platform.file.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * 当前租户的文件数量、占用空间与类型分布。
     * <p>用 CASE 而非 FILTER 聚合，避免 JSqlParser（多租户插件）解析方言语法失败。
     */
    @Select("""
            SELECT COUNT(*) AS total_files,
                   CAST(COALESCE(SUM(file_size), 0) AS BIGINT) AS total_size,
                   COALESCE(SUM(CASE WHEN content_type LIKE 'image/%' THEN 1 ELSE 0 END), 0) AS image_files,
                   COALESCE(SUM(CASE WHEN content_type LIKE 'application/pdf'
                                      OR content_type LIKE 'application/msword'
                                      OR content_type LIKE 'application/vnd.openxmlformats-officedocument%'
                                      OR content_type LIKE 'application/vnd.ms-%'
                                      OR content_type LIKE 'text/%'
                                     THEN 1 ELSE 0 END), 0) AS document_files,
                   MAX(create_time) AS last_upload_time
            FROM sys_file_metadata
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
            """)
    FileStatsDTO selectStats(@Param("tenantId") String tenantId);
}
