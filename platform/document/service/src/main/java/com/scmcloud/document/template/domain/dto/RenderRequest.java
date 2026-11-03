package com.scmcloud.document.template.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RenderRequest {

    /** 要渲染的模板版本 id */
    private String templateVersionId;

    /** 渲染变量数据, key 对应 TemplateVariable.name(支持点路径) */
    private Map<String, Object> data;

    /** 业务类型 */
    private String businessType;

    /** 业务 id */
    private String businessId;

    private String operatorId;
    private String operatorName;
}
