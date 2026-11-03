package com.scmcloud.document.template.domain.dto;

import lombok.Data;

@Data
public class RenderResult {

    private String documentId;
    private Integer documentVersion;
    private String docCode;
    private String status;
}
