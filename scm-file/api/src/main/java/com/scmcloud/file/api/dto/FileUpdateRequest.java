package com.scmcloud.file.api.dto;

import lombok.Data;

/**
 * 文件元数据更新请求（重命名 / 调整业务关联）。
 */
@Data
public class FileUpdateRequest {
    private String originalName;
    private String bizType;
    private String bizId;
}
