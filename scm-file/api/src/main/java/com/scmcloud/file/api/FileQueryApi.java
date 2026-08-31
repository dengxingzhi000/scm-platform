package com.scmcloud.file.api;

import com.scmcloud.file.api.dto.FileMetadataDTO;
import java.util.List;

/**
 * 文件查询 Dubbo 接口
 *
 * @author SCM Platform Team
 * @since 2026-06-18
 */
public interface FileQueryApi {

    /**
     * 根据ID查询文件元数据
     *
     * @param id 文件ID
     * @param tenantId 租户ID
     * @return 文件元数据DTO
     */
    FileMetadataDTO getById(String id, String tenantId);

    /**
     * 根据MD5查询文件元数据
     *
     * @param md5 文件MD5值
     * @param tenantId 租户ID
     * @return 文件元数据DTO
     */
    FileMetadataDTO getByMd5(String md5, String tenantId);

    /**
     * 根据业务关联查询文件列表
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param tenantId 租户ID
     * @return 文件元数据DTO列表
     */
    List<FileMetadataDTO> getByBizId(String bizType, String bizId, String tenantId);

    /**
     * 生成文件预签名URL
     *
     * @param fileKey 文件存储key
     * @param tenantId 租户ID
     * @return 预签名URL
     */
    String generatePresignedUrl(String fileKey, String tenantId);

    /**
     * 下载文件字节(供服务端内部渲染等场景)
     *
     * @param fileKey 文件存储 key
     * @param tenantId 租户ID
     * @return 文件字节
     */
    byte[] download(String fileKey, String tenantId);
}
