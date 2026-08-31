package com.scmcloud.document.template.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("doc_template_version")
public class DocTemplateVersion {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String templateId;

    /** 版本号, 从 1 递增 */
    private Integer version;

    /** 关联的模板源文件(doc_file_metadata.id) */
    private String fileId;

    /** 关联的变量 schema id */
    private String schemaId;

    /** 版本状态: DRAFT / PUBLISHED / ARCHIVED */
    private String status;

    /** 模板文件 checksum, 用于完整性校验 */
    private String checksum;

    private String createdBy;
    private Date createTime;
    private Date publishedAt;
}
