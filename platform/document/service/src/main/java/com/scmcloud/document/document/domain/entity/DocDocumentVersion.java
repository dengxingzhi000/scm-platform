package com.scmcloud.document.document.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("doc_document_version")
public class DocDocumentVersion {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String documentId;

    /** 文档版本号 */
    private Integer version;

    /** 渲染产物文件(doc_file_metadata.id) */
    private String fileId;

    /** 渲染入参的 checksum, 用于追溯 */
    private String renderParamsChecksum;

    private String tenantId;
    private Date createTime;
}
