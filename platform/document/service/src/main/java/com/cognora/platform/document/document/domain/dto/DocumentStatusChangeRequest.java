package com.cognora.platform.document.document.domain.dto;

import lombok.Data;

/**
 * 文档状态流转请求：SIGNED（签署）/ ARCHIVED（归档）。
 */
@Data
public class DocumentStatusChangeRequest {

    /** 目标状态: SIGNED / ARCHIVED */
    private String status;

    private String operatorId;
    private String operatorName;
}
