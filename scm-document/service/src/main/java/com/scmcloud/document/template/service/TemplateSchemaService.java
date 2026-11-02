package com.scmcloud.document.template.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.document.template.domain.dto.TemplateVariable;
import com.scmcloud.document.template.domain.entity.DocTemplateSchema;
import com.scmcloud.document.template.mapper.DocTemplateSchemaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class TemplateSchemaService extends ServiceImpl<DocTemplateSchemaMapper, DocTemplateSchema> {

    public List<DocTemplateSchema> listByTemplate(String templateId) {
        return lambdaQuery()
                .eq(DocTemplateSchema::getTemplateId, templateId)
                .orderByAsc(DocTemplateSchema::getCreateTime)
                .list();
    }

    /**
     * 保存模板变量 Schema：带 id 时更新，否则新建。
     * 变量定义必须是合法 JSON 数组，避免渲染时解析失败。
     */
    @Transactional
    public DocTemplateSchema saveSchema(String templateId, DocTemplateSchema schema) {
        schema.setTemplateId(templateId);
        validateVariables(schema.getVariables());
        if (StringUtils.hasText(schema.getId())) {
            DocTemplateSchema existing = getById(schema.getId());
            if (existing == null || !templateId.equals(existing.getTemplateId())) {
                throw new IllegalArgumentException("template schema not found: " + schema.getId());
            }
            lambdaUpdate()
                    .eq(DocTemplateSchema::getId, schema.getId())
                    .set(schema.getSchemaCode() != null, DocTemplateSchema::getSchemaCode, schema.getSchemaCode())
                    .set(schema.getName() != null, DocTemplateSchema::getName, schema.getName())
                    .set(schema.getDescription() != null, DocTemplateSchema::getDescription, schema.getDescription())
                    .set(schema.getVariables() != null, DocTemplateSchema::getVariables, schema.getVariables())
                    .set(schema.getStatus() != null, DocTemplateSchema::getStatus, schema.getStatus())
                    .update();
            return getById(schema.getId());
        }
        if (!StringUtils.hasText(schema.getSchemaCode())) {
            schema.setSchemaCode("SCHEMA-" + System.currentTimeMillis());
        }
        if (!StringUtils.hasText(schema.getStatus())) {
            schema.setStatus("DRAFT");
        }
        schema.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        schema.setCreateTime(new Date());
        save(schema);
        return schema;
    }

    private void validateVariables(String variables) {
        if (!StringUtils.hasText(variables)) {
            return;
        }
        try {
            List<TemplateVariable> parsed = com.alibaba.fastjson2.JSON.parseArray(variables, TemplateVariable.class);
            for (TemplateVariable variable : parsed) {
                if (!StringUtils.hasText(variable.getName())) {
                    throw new IllegalArgumentException("template variable name is required");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("variables must be a valid JSON array");
        }
    }
}
