package com.scmcloud.document.template.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("doc_template")
public class DocTemplate {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 模板编码, 业务唯一 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 模板类型: CONTRACT / REPORT / NOTICE ... */
    private String templateType;

    /** 分类 */
    private String category;

    /** 文件格式: DOCX / XLSX / PDF / HTML */
    private String fileFormat;

    /** 状态: DRAFT / REVIEWING / PUBLISHED / ARCHIVED */
    private String status;

    /** 当前生效版本号 */
    private Integer currentVersion;

    private String description;

    private String tenantId;
    private String createdBy;
    private Date createTime;
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
