package com.scmcloud.file.api.dto;

import lombok.Data;

import java.util.Date;

/**
 * 文件统计概览。
 */
@Data
public class FileStatsDTO {
    private Long totalFiles;
    private Long totalSize;
    private Long imageFiles;
    private Long documentFiles;
    private Long otherFiles;
    private Date lastUploadTime;
}
