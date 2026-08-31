package com.scmcloud.document.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.document.template.domain.entity.DocTemplate;
import com.scmcloud.document.template.domain.entity.DocTemplateVersion;
import com.scmcloud.document.template.mapper.DocTemplateMapper;
import com.scmcloud.document.template.mapper.DocTemplateVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TemplateService extends ServiceImpl<DocTemplateMapper, DocTemplate> {

    private final DocTemplateVersionMapper versionMapper;

    public TemplateService(DocTemplateVersionMapper versionMapper) {
        this.versionMapper = versionMapper;
    }

    public DocTemplateVersion getVersion(String id) {
        return versionMapper.selectById(id);
    }

    public List<DocTemplateVersion> listVersions(String templateId) {
        return versionMapper.selectList(new LambdaQueryWrapper<DocTemplateVersion>()
                .eq(DocTemplateVersion::getTemplateId, templateId));
    }

    @Transactional
    public DocTemplateVersion publishVersion(DocTemplateVersion version) {
        versionMapper.insert(version);
        lambdaUpdate()
                .eq(DocTemplate::getId, version.getTemplateId())
                .set(DocTemplate::getCurrentVersion, version.getVersion())
                .set(DocTemplate::getStatus, "PUBLISHED")
                .update();
        return version;
    }
}
