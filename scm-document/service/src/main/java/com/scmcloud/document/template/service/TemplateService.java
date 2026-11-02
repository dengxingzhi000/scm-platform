package com.scmcloud.document.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.document.template.domain.entity.DocTemplate;
import com.scmcloud.document.template.domain.entity.DocTemplateVersion;
import com.scmcloud.document.template.mapper.DocTemplateMapper;
import com.scmcloud.document.template.mapper.DocTemplateVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TemplateService extends ServiceImpl<DocTemplateMapper, DocTemplate> {

    private final DocTemplateVersionMapper versionMapper;

    public TemplateService(DocTemplateVersionMapper versionMapper) {
        this.versionMapper = versionMapper;
    }

    public DocTemplateVersion getVersion(String id) {
        DocTemplateVersion version = versionMapper.selectById(id);
        if (version == null || !currentTenantId().equals(version.getTenantId())) {
            return null;
        }
        return version;
    }

    /**
     * 按 ID 查询当前租户的模板；不存在或属于其他租户时返回空。
     */
    public Optional<DocTemplate> findCurrentTenant(String id) {
        DocTemplate template = getById(id);
        if (template == null || !currentTenantId().equals(template.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(template);
    }

    /**
     * 查询当前租户模板的版本列表；模板不存在或不属于当前租户时返回空列表。
     */
    public List<DocTemplateVersion> listVersions(String templateId) {
        if (findCurrentTenant(templateId).isEmpty()) {
            return List.of();
        }
        return versionMapper.selectList(new LambdaQueryWrapper<DocTemplateVersion>()
                .eq(DocTemplateVersion::getTemplateId, templateId)
                .eq(DocTemplateVersion::getTenantId, currentTenantId())
                .orderByDesc(DocTemplateVersion::getVersion));
    }

    /**
     * 分页查询当前租户的模板，支持按名称/编码关键字、类型、分类和状态过滤。
     */
    public Page<DocTemplate> pageQuery(int page, int size, String keyword,
                                       String templateType, String category, String status) {
        LambdaQueryWrapper<DocTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocTemplate::getTenantId, currentTenantId());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DocTemplate::getTemplateName, keyword)
                    .or().like(DocTemplate::getTemplateCode, keyword));
        }
        wrapper.eq(StringUtils.hasText(templateType), DocTemplate::getTemplateType, templateType)
                .eq(StringUtils.hasText(category), DocTemplate::getCategory, category)
                .eq(StringUtils.hasText(status), DocTemplate::getStatus, status)
                .orderByDesc(DocTemplate::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 创建模板。租户来自当前上下文，初始状态为草稿。
     */
    @Transactional
    public DocTemplate createTemplate(DocTemplate template) {
        template.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        if (!StringUtils.hasText(template.getStatus())) {
            template.setStatus("DRAFT");
        }
        if (template.getCurrentVersion() == null) {
            template.setCurrentVersion(0);
        }
        save(template);
        return template;
    }

    /**
     * 更新模板基础信息。仅覆盖请求中非空的字段。
     */
    @Transactional
    public DocTemplate updateTemplate(String id, DocTemplate patch) {
        findCurrentTenant(id)
                .orElseThrow(() -> new IllegalArgumentException("document template not found: " + id));
        lambdaUpdate()
                .eq(DocTemplate::getId, id)
                .set(patch.getTemplateName() != null, DocTemplate::getTemplateName, patch.getTemplateName())
                .set(patch.getTemplateType() != null, DocTemplate::getTemplateType, patch.getTemplateType())
                .set(patch.getCategory() != null, DocTemplate::getCategory, patch.getCategory())
                .set(patch.getFileFormat() != null, DocTemplate::getFileFormat, patch.getFileFormat())
                .set(patch.getDescription() != null, DocTemplate::getDescription, patch.getDescription())
                .set(patch.getStatus() != null, DocTemplate::getStatus, patch.getStatus())
                .update();
        return getById(id);
    }

    /**
     * 创建模板版本。版本号自动递增，初始状态为草稿。
     */
    @Transactional
    public DocTemplateVersion createVersion(String templateId, DocTemplateVersion request) {
        findCurrentTenant(templateId)
                .orElseThrow(() -> new IllegalArgumentException("document template not found: " + templateId));
        DocTemplateVersion version = new DocTemplateVersion();
        version.setTemplateId(templateId);
        version.setVersion(nextVersionNo(templateId));
        version.setFileId(request.getFileId());
        version.setSchemaId(request.getSchemaId());
        version.setChecksum(request.getChecksum());
        version.setStatus("DRAFT");
        version.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        version.setCreateTime(new Date());
        versionMapper.insert(version);
        return version;
    }

    private int nextVersionNo(String templateId) {
        DocTemplateVersion latest = versionMapper.selectOne(new LambdaQueryWrapper<DocTemplateVersion>()
                .eq(DocTemplateVersion::getTemplateId, templateId)
                .orderByDesc(DocTemplateVersion::getVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
    }

    /**
     * 发布模板版本：归档其他已发布版本，并把模板主记录切到该版本。
     */
    @Transactional
    public DocTemplateVersion publishVersion(String templateId, String versionId) {
        findCurrentTenant(templateId)
                .orElseThrow(() -> new IllegalArgumentException("document template not found: " + templateId));
        DocTemplateVersion version = versionMapper.selectById(versionId);
        if (version == null
                || !templateId.equals(version.getTemplateId())
                || !currentTenantId().equals(version.getTenantId())) {
            throw new IllegalArgumentException("template version not found: " + versionId);
        }
        if (!StringUtils.hasText(version.getFileId())) {
            throw new IllegalStateException("template version has no source file, upload the template file first");
        }
        versionMapper.update(null, new LambdaUpdateWrapper<DocTemplateVersion>()
                .eq(DocTemplateVersion::getTemplateId, templateId)
                .eq(DocTemplateVersion::getStatus, "PUBLISHED")
                .ne(DocTemplateVersion::getId, versionId)
                .set(DocTemplateVersion::getStatus, "ARCHIVED"));
        version.setStatus("PUBLISHED");
        version.setPublishedAt(new Date());
        versionMapper.updateById(version);
        lambdaUpdate()
                .eq(DocTemplate::getId, templateId)
                .set(DocTemplate::getCurrentVersion, version.getVersion())
                .set(DocTemplate::getStatus, "PUBLISHED")
                .update();
        return version;
    }

    private String currentTenantId() {
        return TenantContextHolder.getRequiredTenantId().toString();
    }
}
