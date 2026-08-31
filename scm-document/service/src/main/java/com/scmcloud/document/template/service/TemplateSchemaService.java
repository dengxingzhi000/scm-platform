package com.scmcloud.document.template.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.document.template.domain.entity.DocTemplateSchema;
import com.scmcloud.document.template.mapper.DocTemplateSchemaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TemplateSchemaService extends ServiceImpl<DocTemplateSchemaMapper, DocTemplateSchema> {

    public List<DocTemplateSchema> listByTemplate(String templateId) {
        return lambdaQuery()
                .eq(DocTemplateSchema::getTemplateId, templateId)
                .list();
    }
}
