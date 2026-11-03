package com.scmcloud.file.api.dto;

import lombok.Data;
import java.util.Date;

@Data
public class FileMetadataDTO {
    private String id;
    private String originalName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private String storageEngine;
    private String md5;
    private Integer version;
    private String bizType;
    private String bizId;
    private String status;
    private Integer refCount;
    private String tenantId;
    private String createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
}
