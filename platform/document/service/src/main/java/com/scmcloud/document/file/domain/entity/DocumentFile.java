package com.scmcloud.document.file.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("doc_file_metadata")
public class DocumentFile {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 文档产物名称(展示用, 如 "劳动合同-张三-v3.pdf") */
    private String originalName;

    /** 文件来源: TEMPLATE_SOURCE / RENDERED_OUTPUT */
    private String sourceType;

    /** 关联业务 id(模板版本 id / 文档版本 id) */
    private String refId;

    /** 指向 scm-file 的文件 id, 字节始终存于 scm-file */
    private String fileRef;

    private String tenantId;
    private String createBy;
    private Date createTime;
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
