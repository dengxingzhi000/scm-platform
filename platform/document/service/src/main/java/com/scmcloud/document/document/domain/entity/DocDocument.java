package com.scmcloud.document.document.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("doc_document")
public class DocDocument {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 文档编码 */
    private String docCode;

    /** 文档名称 */
    private String docName;

    /** 来源模板 id(固化引用, 不随模板升级变化) */
    private String templateId;

    /** 来源模板版本号(固化) */
    private Integer templateVersion;

    /** 当前生效文档版本号 */
    private Integer currentVersion;

    /** 状态: GENERATED / SIGNED / ARCHIVED */
    private String status;

    /** 业务类型 */
    private String businessType;

    /** 业务 id */
    private String businessId;

    private String tenantId;
    private String createdBy;
    private Date createTime;
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
