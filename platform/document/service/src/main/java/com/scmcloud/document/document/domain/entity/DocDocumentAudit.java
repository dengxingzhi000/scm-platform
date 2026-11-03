package com.scmcloud.document.document.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("doc_document_audit")
public class DocDocumentAudit {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String documentId;

    /** 操作类型: GENERATE / RENDER / PREVIEW / SIGN / ARCHIVE */
    private String operation;

    private String operatorId;
    private String operatorName;

    private String ip;
    private String userAgent;

    /** 操作前后版本, 用于追溯 */
    private Integer beforeVersion;
    private Integer afterVersion;

    private String traceId;

    private String tenantId;
    private Date createTime;
}
