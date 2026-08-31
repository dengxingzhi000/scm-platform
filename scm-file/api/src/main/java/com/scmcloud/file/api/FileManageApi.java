package com.scmcloud.file.api;

import com.scmcloud.file.api.dto.FileMetadataDTO;

/**
 * 文件管理 Dubbo 接口
 *
 * @author SCM Platform Team
 * @since 2026-06-18
 */
public interface FileManageApi {

    /**
     * 删除文件
     *
     * @param id 文件ID
     * @param tenantId 租户ID
     */
    void delete(String id, String tenantId);

    /**
     * 更新文件业务关联
     *
     * @param id 文件ID
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param tenantId 租户ID
     */
    void updateBizAssociation(String id, String bizType, String bizId, String tenantId);

    /**
     * 服务端字节上传(供文档渲染产物等场景, 不经过 MultipartFile)
     *
     * @return 文件元数据
     */
    FileMetadataDTO upload(byte[] content, String originalName, String contentType,
                           String tenantId, String bizType, String bizId);
}
