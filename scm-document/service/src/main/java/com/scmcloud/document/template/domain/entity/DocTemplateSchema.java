package com.scmcloud.document.template.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.scmcloud.document.template.domain.dto.TemplateVariable;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
@TableName("doc_template_schema")
public class DocTemplateSchema {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String templateId;

    /** 变量 schema 编码, 业务唯一 */
    private String schemaCode;

    private String name;

    private String description;

    /** 变量定义(JSON 数组), 如 [{"name":"user.name","type":"String","required":true}] */
    private String variables;

    /** 状态: DRAFT / PUBLISHED */
    private String status;

    private String tenantId;
    private String createdBy;
    private Date createTime;

    /** 解析变量定义(非持久化字段, MP 仅映射成员变量) */
    public List<TemplateVariable> parseVariables() {
        if (variables == null || variables.isBlank()) {
            return Collections.emptyList();
        }
        return com.alibaba.fastjson2.JSON.parseArray(variables, TemplateVariable.class);
    }
}
