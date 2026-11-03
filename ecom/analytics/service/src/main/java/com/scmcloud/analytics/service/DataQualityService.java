package com.scmcloud.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.domain.entity.DataQualityResult;
import com.scmcloud.analytics.domain.entity.DataQualityRule;
import com.scmcloud.analytics.mapper.DataQualityResultMapper;
import com.scmcloud.analytics.mapper.DataQualityRuleMapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * CRUD for data-quality rules plus rule execution and result persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityService {

    private static final Set<String> RULE_TYPES = Set.of("NOT_NULL", "RANGE", "UNIQUE", "FRESHNESS");
    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH");

    private final DataQualityRuleMapper ruleMapper;
    private final DataQualityResultMapper resultMapper;
    private final DataQualityEvaluator evaluator;

    @Master(reason = "Create quality rule")
    @Transactional(rollbackFor = Exception.class)
    public DataQualityRule create(DataQualityRule rule) {
        validate(rule);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DataQualityRule entity = DataQualityRule.builder()
                .tenantId(tenantId)
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .datasetCode(rule.getDatasetCode())
                .ruleType(rule.getRuleType().toUpperCase())
                .targetField(rule.getTargetField())
                .config(rule.getConfig() == null ? Map.of() : rule.getConfig())
                .severity(rule.getSeverity() == null ? "MEDIUM" : rule.getSeverity().toUpperCase())
                .enabled(rule.getEnabled() == null || rule.getEnabled())
                .status(rule.getStatus() == null ? 1 : rule.getStatus())
                .createTime(now)
                .updateTime(now)
                .deleted(false)
                .remark(rule.getRemark())
                .build();
        try {
            ruleMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("Quality rule code already exists: " + rule.getRuleCode());
        }
        log.info("Created quality rule: code={}, dataset={}, type={}",
                (entity.getRuleCode() + "").replace("\r", "_").replace("\n", "_"), (entity.getDatasetCode() + "").replace("\r", "_").replace("\n", "_"), (entity.getRuleType() + "").replace("\r", "_").replace("\n", "_"));
        return entity;
    }

    @Master(reason = "Update quality rule")
    @Transactional(rollbackFor = Exception.class)
    public DataQualityRule update(UUID id, DataQualityRule rule) {
        validate(rule);
        DataQualityRule existing = requireOwnedRule(id);
        existing.setRuleCode(rule.getRuleCode());
        existing.setRuleName(rule.getRuleName());
        existing.setDescription(rule.getDescription());
        existing.setDatasetCode(rule.getDatasetCode());
        existing.setRuleType(rule.getRuleType().toUpperCase());
        existing.setTargetField(rule.getTargetField());
        if (rule.getConfig() != null) {
            existing.setConfig(rule.getConfig());
        }
        if (rule.getSeverity() != null) {
            existing.setSeverity(rule.getSeverity().toUpperCase());
        }
        if (rule.getEnabled() != null) {
            existing.setEnabled(rule.getEnabled());
        }
        if (rule.getStatus() != null) {
            existing.setStatus(rule.getStatus());
        }
        if (rule.getRemark() != null) {
            existing.setRemark(rule.getRemark());
        }
        existing.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
        try {
            ruleMapper.updateById(existing);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("Quality rule code already exists: " + rule.getRuleCode());
        }
        return existing;
    }

    @Master(reason = "Delete quality rule")
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        requireOwnedRule(id);
        ruleMapper.deleteById(id);
    }

    @Slave("Read quality rule")
    public DataQualityRule getById(UUID id) {
        return requireOwnedRule(id);
    }

    @Slave("List quality rules")
    public Page<DataQualityRule> list(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<DataQualityRule> wrapper = Wrappers.lambdaQuery(DataQualityRule.class)
                .eq(DataQualityRule::getTenantId, TenantContextHolder.getRequiredTenantId().toString());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DataQualityRule::getRuleCode, keyword)
                    .or().like(DataQualityRule::getRuleName, keyword));
        }
        wrapper.orderByDesc(DataQualityRule::getCreateTime);
        return ruleMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Master(reason = "Run quality rule and persist result")
    @Transactional(rollbackFor = Exception.class)
    public DataQualityResult runRule(UUID id) {
        DataQualityRule rule = requireOwnedRule(id);
        if (!Boolean.TRUE.equals(rule.getEnabled()) || (rule.getStatus() != null && rule.getStatus() == 0)) {
            throw new BusinessException("Quality rule is disabled: " + rule.getRuleCode());
        }
        return execute(rule);
    }

    @Master(reason = "Persist quality result")
    @Transactional(rollbackFor = Exception.class)
    public DataQualityResult execute(DataQualityRule rule) {
        DataQualityEvaluator.QualityCheckResult check = evaluator.evaluate(rule);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        DataQualityResult result = DataQualityResult.builder()
                .tenantId(tenantId)
                .ruleId(rule.getId())
                .ruleCode(rule.getRuleCode())
                .datasetCode(rule.getDatasetCode())
                .checkDate(LocalDate.now(ZoneOffset.UTC))
                .passed(check.passed())
                .metricValue(check.metricValue())
                .message(check.message())
                .createTime(OffsetDateTime.now(ZoneOffset.UTC))
                .deleted(false)
                .build();
        resultMapper.insert(result);
        return result;
    }

    @Master(reason = "Persist failed quality check")
    @Transactional(rollbackFor = Exception.class)
    public DataQualityResult recordFailure(DataQualityRule rule, String message) {
        DataQualityResult result = DataQualityResult.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId().toString())
                .ruleId(rule.getId())
                .ruleCode(rule.getRuleCode())
                .datasetCode(rule.getDatasetCode())
                .checkDate(LocalDate.now(ZoneOffset.UTC))
                .passed(false)
                .metricValue(null)
                .message("Check error: " + message)
                .createTime(OffsetDateTime.now(ZoneOffset.UTC))
                .deleted(false)
                .build();
        resultMapper.insert(result);
        return result;
    }

    @Slave("List quality results")
    public List<DataQualityResult> listResults(UUID ruleId, LocalDate checkDate) {
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        return checkDate == null
                ? resultMapper.selectByRule(tenantId, ruleId)
                : resultMapper.selectByRuleAndDate(tenantId, ruleId, checkDate);
    }

    private DataQualityRule requireOwnedRule(UUID id) {
        DataQualityRule rule = ruleMapper.selectById(id);
        String tenantId = TenantContextHolder.getRequiredTenantId().toString();
        if (rule == null || !Objects.equals(tenantId, rule.getTenantId())) {
            throw new BusinessException("Quality rule not found: " + id);
        }
        return rule;
    }

    private void validate(DataQualityRule rule) {
        if (rule == null) {
            throw new BusinessException("Quality rule must not be null");
        }
        if (rule.getRuleCode() == null || rule.getRuleCode().isBlank()) {
            throw new BusinessException("ruleCode is required");
        }
        if (rule.getRuleName() == null || rule.getRuleName().isBlank()) {
            throw new BusinessException("ruleName is required");
        }
        if (rule.getDatasetCode() == null || rule.getDatasetCode().isBlank()) {
            throw new BusinessException("datasetCode is required");
        }
        if (rule.getTargetField() == null || rule.getTargetField().isBlank()) {
            throw new BusinessException("targetField is required");
        }
        if (rule.getRuleType() == null || !RULE_TYPES.contains(rule.getRuleType().toUpperCase())) {
            throw new BusinessException("ruleType must be one of " + RULE_TYPES);
        }
        if (rule.getSeverity() != null && !SEVERITIES.contains(rule.getSeverity().toUpperCase())) {
            throw new BusinessException("severity must be one of " + SEVERITIES);
        }
        if (rule.getStatus() != null && rule.getStatus() != 0 && rule.getStatus() != 1) {
            throw new BusinessException("status must be 0 or 1");
        }
    }
}
