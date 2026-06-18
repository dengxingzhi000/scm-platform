package com.scmcloud.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.scmcloud.file.api.enums.UploadTaskStatus;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_upload_task")
public class UploadTask {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fileName;
    private Long fileSize;
    private String md5;
    private String storageKey;
    private Integer totalParts;
    private Integer completedParts;
    private UploadTaskStatus status;
    private String uploadId;
    private Long tenantId;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer deleted;
    @Version
    private Integer lockVersion;
}
