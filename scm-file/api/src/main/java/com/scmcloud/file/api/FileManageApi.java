package com.scmcloud.file.api;

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
    void delete(String id, Long tenantId);

    /**
     * 更新文件业务关联
     *
     * @param id 文件ID
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param tenantId 租户ID
     */
    void updateBizAssociation(String id, String bizType, String bizId, Long tenantId);
}
