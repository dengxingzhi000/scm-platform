package com.scmcloud.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_file_version")
public class FileVersion {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fileId;
    private Integer version;
    private String storageKey;
    private Long fileSize;
    private String md5;
    private String createBy;
    private Date createTime;
    private String tenantId;
}
