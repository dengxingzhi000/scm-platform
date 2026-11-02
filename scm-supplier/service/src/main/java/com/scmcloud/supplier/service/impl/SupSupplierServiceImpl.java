package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierCategory;
import com.scmcloud.supplier.domain.entity.SupSupplierCategoryRel;
import com.scmcloud.supplier.domain.entity.SupSupplierChangeLog;
import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;
import com.scmcloud.supplier.domain.support.SupplierScoreCalculator;
import com.scmcloud.supplier.dto.SupplierAuditRequest;
import com.scmcloud.supplier.dto.SupplierPerformanceVO;
import com.scmcloud.supplier.dto.SupplierProfileVO;
import com.scmcloud.supplier.dto.SupplierStatsVO;
import com.scmcloud.supplier.dto.SupplierUpsertRequest;
import com.scmcloud.supplier.mapper.SupSupplierCategoryRelMapper;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import com.scmcloud.supplier.service.ISupSupplierBankAccountService;
import com.scmcloud.supplier.service.ISupSupplierCategoryService;
import com.scmcloud.supplier.service.ISupSupplierCertificateService;
import com.scmcloud.supplier.service.ISupSupplierChangeLogService;
import com.scmcloud.supplier.service.ISupSupplierContactService;
import com.scmcloud.supplier.service.ISupSupplierEvaluationService;
import com.scmcloud.supplier.service.ISupSupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierServiceImpl extends ServiceImpl<SupSupplierMapper, SupSupplier>
        implements ISupSupplierService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,64}$");
    private static final Set<String> CREDIT_RATINGS = Set.of("A+", "A", "B+", "B", "C", "D");
    private static final int EXPIRY_WARNING_DAYS = 30;

    private final ISupSupplierContactService contactService;
    private final ISupSupplierBankAccountService bankAccountService;
    private final ISupSupplierCertificateService certificateService;
    private final ISupSupplierCategoryService categoryService;
    private final SupSupplierCategoryRelMapper categoryRelMapper;
    private final ISupSupplierChangeLogService changeLogService;
    private final ISupSupplierEvaluationService evaluationService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Page<SupSupplier> pageList(int page, int size, String keyword, Integer supplierType,
                                      Integer cooperationStatus, Boolean enabled, Integer auditStatus,
                                      Integer supplierLevel, Integer riskLevel, Boolean blacklisted,
                                      String categoryId) {
        log.debug("分页查询供应商 page={}, size={}, keyword={}", page, size, keyword);

        LambdaQueryWrapper<SupSupplier> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SupSupplier::getSupplierName, keyword)
                    .or().like(SupSupplier::getSupplierCode, keyword)
                    .or().like(SupSupplier::getContactName, keyword)
            );
        }
        if (supplierType != null) {
            wrapper.eq(SupSupplier::getSupplierType, supplierType);
        }
        if (cooperationStatus != null) {
            wrapper.eq(SupSupplier::getCooperationStatus, cooperationStatus);
        }
        if (enabled != null) {
            wrapper.eq(SupSupplier::getEnabled, enabled);
        }
        if (auditStatus != null) {
            wrapper.eq(SupSupplier::getAuditStatus, auditStatus);
        }
        if (supplierLevel != null) {
            wrapper.eq(SupSupplier::getSupplierLevel, supplierLevel);
        }
        if (riskLevel != null) {
            wrapper.eq(SupSupplier::getRiskLevel, riskLevel);
        }
        if (blacklisted != null) {
            wrapper.eq(SupSupplier::getBlacklisted, blacklisted);
        }
        if (StringUtils.hasText(categoryId)) {
            List<String> supplierIds = categoryRelMapper.selectList(
                            Wrappers.<SupSupplierCategoryRel>lambdaQuery()
                                    .eq(SupSupplierCategoryRel::getCategoryId, categoryId))
                    .stream()
                    .map(SupSupplierCategoryRel::getSupplierId)
                    .distinct()
                    .toList();
            if (supplierIds.isEmpty()) {
                Page<SupSupplier> emptyPage = new Page<>(page, size);
                emptyPage.setTotal(0);
                return emptyPage;
            }
            wrapper.in(SupSupplier::getId, supplierIds);
        }

        wrapper.eq(SupSupplier::getDeleted, false);
        wrapper.orderByAsc(SupSupplier::getSortOrder);
        wrapper.orderByDesc(SupSupplier::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupSupplier> listActive() {
        log.debug("查询所有可合作供应商");
        return lambdaQuery()
                .eq(SupSupplier::getEnabled, true)
                .eq(SupSupplier::getCooperationStatus, 1)
                .eq(SupSupplier::getAuditStatus, 2)
                .eq(SupSupplier::getBlacklisted, false)
                .eq(SupSupplier::getDeleted, false)
                .orderByAsc(SupSupplier::getSortOrder)
                .list();
    }

    @Override
    public List<SupSupplier> searchByName(String name) {
        if (!StringUtils.hasText(name)) {
            return List.of();
        }
        log.debug("按名称搜索供应商: name={}", name);
        return lambdaQuery()
                .like(SupSupplier::getSupplierName, name)
                .eq(SupSupplier::getDeleted, false)
                .last("LIMIT 20")
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplier createSupplier(SupplierUpsertRequest request) {
        validateMasterData(request);

        String suppliedCode = normalizeCode(request.getSupplierCode());
        String taxNo = normalizeTaxNo(request.getTaxNo());
        ensureTaxNoUnique(taxNo, null);
        if (StringUtils.hasText(suppliedCode)) {
            ensureCodeUnique(suppliedCode, null);
        }

        String code = StringUtils.hasText(suppliedCode) ? suppliedCode : generateSupplierCode();
        SupSupplier supplier = new SupSupplier();
        BeanUtils.copyProperties(request, supplier);
        supplier.setSupplierCode(code);
        supplier.setTaxNo(taxNo);
        supplier.setId(UUID.randomUUID().toString());
        supplier.setAuditStatus(0);
        supplier.setEnabled(false);
        supplier.setCooperationStatus(0);
        supplier.setSupplierLevel(4);
        supplier.setRiskLevel(1);
        supplier.setBlacklisted(false);
        supplier.setDeleted(false);
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());
        try {
            save(supplier);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商编码已存在");
        }
        changeLogService.record(supplier.getId(), "CREATE",
                "创建供应商档案，编码 " + code, null, null, null);
        log.info("创建供应商成功 id={}, code={}", supplier.getId(), code);
        return supplier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplier updateSupplier(String id, SupplierUpsertRequest request) {
        SupSupplier existing = requireSupplier(id);
        validateMasterData(request);

        String code = normalizeCode(request.getSupplierCode());
        String taxNo = normalizeTaxNo(request.getTaxNo());
        if (StringUtils.hasText(code)) {
            ensureCodeUnique(code, id);
        }
        ensureTaxNoUnique(taxNo, id);

        SupSupplier update = new SupSupplier();
        BeanUtils.copyProperties(request, update);
        update.setSupplierCode(code);
        update.setTaxNo(taxNo);
        update.setId(id);
        update.setUpdateTime(LocalDateTime.now());
        updateById(update);

        SupSupplier current = getById(id);
        changeLogService.record(id, "UPDATE", describeChanges(existing, current),
                null, null, null);
        return current;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(String id, String operatorId, String operatorName) {
        SupSupplier existing = requireSupplier(id);
        if (Integer.valueOf(1).equals(existing.getCooperationStatus())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST,
                    "合作中的供应商不能删除，请先终止合作");
        }
        removeById(id);
        changeLogService.record(id, "DELETE", "删除供应商档案", null, operatorId, operatorName);
        log.info("删除供应商 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableSupplier(String id) {
        SupSupplier supplier = requireSupplier(id);
        if (!Integer.valueOf(2).equals(supplier.getAuditStatus())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "仅准入审核通过的供应商可启用");
        }
        if (Boolean.TRUE.equals(supplier.getBlacklisted())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "黑名单供应商不可启用");
        }
        if (Boolean.TRUE.equals(supplier.getEnabled())) {
            return true;
        }
        supplier.setEnabled(true);
        supplier.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(supplier);
        if (success) {
            changeLogService.record(id, "ENABLE", "启用供应商", null, null, null);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableSupplier(String id) {
        SupSupplier supplier = requireSupplier(id);
        if (Boolean.FALSE.equals(supplier.getEnabled())) {
            return true;
        }
        supplier.setEnabled(false);
        supplier.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(supplier);
        if (success) {
            changeLogService.record(id, "DISABLE", "停用供应商", null, null, null);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAudit(String id, String operatorId, String operatorName) {
        SupSupplier supplier = requireSupplier(id);
        if (!Integer.valueOf(0).equals(supplier.getAuditStatus())
                && !Integer.valueOf(3).equals(supplier.getAuditStatus())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "当前准入状态不可提交审核");
        }
        if (!StringUtils.hasText(supplier.getTaxNo())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "提交准入审核前需补全税号");
        }
        supplier.setAuditStatus(1);
        supplier.setAuditRemark(null);
        supplier.setUpdateTime(LocalDateTime.now());
        updateById(supplier);
        changeLogService.record(id, "SUBMIT_AUDIT", "提交准入审核", null, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(String id, SupplierAuditRequest request) {
        SupSupplier supplier = requireSupplier(id);
        if (!Integer.valueOf(1).equals(supplier.getAuditStatus())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "仅待审核状态的供应商可执行审核");
        }
        String action = request.getAction() == null ? "" : request.getAction().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(action)) {
            if (Boolean.TRUE.equals(supplier.getBlacklisted())) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "黑名单供应商不可通过准入审核");
            }
            supplier.setAuditStatus(2);
            supplier.setEnabled(true);
            supplier.setAuditedBy(request.getOperatorId());
            supplier.setAuditedAt(LocalDateTime.now());
            supplier.setAuditRemark(request.getRemark());
            supplier.setUpdateTime(LocalDateTime.now());
            updateById(supplier);
            changeLogService.record(id, "AUDIT_APPROVE", "准入审核通过",
                    request.getRemark(), request.getOperatorId(), request.getOperatorName());
        } else if ("REJECT".equals(action)) {
            if (!StringUtils.hasText(request.getRemark())) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "驳回准入必须填写原因");
            }
            supplier.setAuditStatus(3);
            supplier.setEnabled(false);
            supplier.setAuditedBy(request.getOperatorId());
            supplier.setAuditedAt(LocalDateTime.now());
            supplier.setAuditRemark(request.getRemark());
            supplier.setUpdateTime(LocalDateTime.now());
            updateById(supplier);
            changeLogService.record(id, "AUDIT_REJECT", "准入审核驳回",
                    request.getRemark(), request.getOperatorId(), request.getOperatorName());
        } else {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "审核动作仅支持 APPROVE/REJECT");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void blacklist(String id, String reason, String operatorId, String operatorName) {
        SupSupplier supplier = requireSupplier(id);
        if (!StringUtils.hasText(reason)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "列入黑名单必须填写原因");
        }
        supplier.setBlacklisted(true);
        supplier.setBlacklistReason(reason);
        supplier.setBlacklistedAt(LocalDateTime.now());
        supplier.setEnabled(false);
        supplier.setCooperationStatus(3);
        supplier.setUpdateTime(LocalDateTime.now());
        updateById(supplier);
        changeLogService.record(id, "BLACKLIST", "列入黑名单", reason, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblacklist(String id, String operatorId, String operatorName) {
        SupSupplier supplier = requireSupplier(id);
        if (Boolean.FALSE.equals(supplier.getBlacklisted())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商不在黑名单中");
        }
        LambdaUpdateWrapper<SupSupplier> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SupSupplier::getId, id);
        wrapper.set(SupSupplier::getBlacklisted, false);
        wrapper.set(SupSupplier::getBlacklistReason, null);
        wrapper.set(SupSupplier::getBlacklistedAt, null);
        wrapper.set(SupSupplier::getUpdateTime, LocalDateTime.now());
        update(null, wrapper);
        changeLogService.record(id, "UNBLACKLIST", "移出黑名单", null, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeRiskLevel(String id, Integer riskLevel, String reason,
                                String operatorId, String operatorName) {
        if (riskLevel == null || riskLevel < 1 || riskLevel > 3) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "风险等级仅支持 1-低,2-中,3-高");
        }
        SupSupplier supplier = requireSupplier(id);
        Integer before = supplier.getRiskLevel();
        if (riskLevel.equals(before)) {
            return;
        }
        supplier.setRiskLevel(riskLevel);
        supplier.setUpdateTime(LocalDateTime.now());
        updateById(supplier);
        changeLogService.record(id, "RISK_LEVEL",
                "风险等级 " + before + " -> " + riskLevel, reason, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeCooperationStatus(String id, Integer status, String operatorId, String operatorName) {
        if (status == null || status < 0 || status > 3) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "合作状态仅支持 0-3");
        }
        SupSupplier supplier = requireSupplier(id);
        int current = supplier.getCooperationStatus() == null ? 0 : supplier.getCooperationStatus();
        if (current == status) {
            return;
        }
        boolean allowed = switch (current) {
            case 0 -> status == 1;
            case 1 -> status == 2 || status == 3;
            case 2 -> status == 1 || status == 3;
            case 3 -> status == 0;
            default -> false;
        };
        if (!allowed) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST,
                    "非法合作状态流转: " + current + " -> " + status);
        }
        if (status == 1) {
            if (!Integer.valueOf(2).equals(supplier.getAuditStatus())) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "未通过准入审核的供应商不可建立合作");
            }
            if (Boolean.TRUE.equals(supplier.getBlacklisted())) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "黑名单供应商不可建立合作");
            }
            if (supplier.getCooperationStartDate() == null) {
                supplier.setCooperationStartDate(LocalDate.now());
            }
        }
        Integer before = supplier.getCooperationStatus();
        supplier.setCooperationStatus(status);
        supplier.setUpdateTime(LocalDateTime.now());
        updateById(supplier);
        changeLogService.record(id, "COOPERATION_STATUS",
                "合作状态 " + before + " -> " + status, null, operatorId, operatorName);
    }

    @Override
    public SupplierProfileVO getProfile(String id) {
        SupSupplier supplier = requireSupplier(id);
        SupplierProfileVO profile = new SupplierProfileVO();
        profile.setSupplier(supplier);
        profile.setContacts(contactService.listBySupplierId(id));
        profile.setBankAccounts(bankAccountService.listBySupplierId(id));
        profile.setCertificates(certificateService.listBySupplierId(id));
        profile.setCategories(getCategories(id));
        profile.setChangeLogs(changeLogService.listBySupplierId(id, 20));
        return profile;
    }

    @Override
    public SupplierStatsVO getStats() {
        SupplierStatsVO stats = new SupplierStatsVO();
        stats.setTotal(lambdaQuery().count());
        stats.setEnabled(lambdaQuery().eq(SupSupplier::getEnabled, true).count());
        stats.setCooperating(lambdaQuery().eq(SupSupplier::getCooperationStatus, 1).count());
        stats.setPendingAudit(lambdaQuery().eq(SupSupplier::getAuditStatus, 1).count());
        stats.setBlacklisted(lambdaQuery().eq(SupSupplier::getBlacklisted, true).count());
        stats.setExpiringCertificates(certificateService.countExpiring(EXPIRY_WARNING_DAYS));

        Map<Integer, Long> cooperation = new LinkedHashMap<>();
        for (int status = 0; status <= 3; status++) {
            cooperation.put(status, lambdaQuery().eq(SupSupplier::getCooperationStatus, status).count());
        }
        stats.setByCooperationStatus(cooperation);

        Map<Integer, Long> audit = new LinkedHashMap<>();
        for (int status = 0; status <= 3; status++) {
            audit.put(status, lambdaQuery().eq(SupSupplier::getAuditStatus, status).count());
        }
        stats.setByAuditStatus(audit);
        return stats;
    }

    @Override
    public SupplierPerformanceVO getPerformance(String id) {
        SupSupplier supplier = requireSupplier(id);
        List<SupSupplierEvaluation> evaluations = evaluationService.listBySupplierId(id);

        List<BigDecimal> quality = evaluations.stream().map(SupSupplierEvaluation::getQualityScore).toList();
        List<BigDecimal> delivery = evaluations.stream().map(SupSupplierEvaluation::getDeliveryScore).toList();
        List<BigDecimal> service = evaluations.stream().map(SupSupplierEvaluation::getServiceScore).toList();
        List<BigDecimal> price = evaluations.stream().map(SupSupplierEvaluation::getPriceScore).toList();

        BigDecimal qualityAvg = SupplierScoreCalculator.average(quality);
        BigDecimal deliveryAvg = SupplierScoreCalculator.average(delivery);
        BigDecimal serviceAvg = SupplierScoreCalculator.average(service);
        BigDecimal priceAvg = SupplierScoreCalculator.average(price);
        BigDecimal overall = SupplierScoreCalculator.weightedTotal(qualityAvg, deliveryAvg, serviceAvg, priceAvg);

        SupplierPerformanceVO performance = new SupplierPerformanceVO();
        performance.setSupplierId(id);
        performance.setSupplierName(supplier.getSupplierName());
        performance.setSupplierLevel(supplier.getSupplierLevel());
        performance.setOverallScore(overall);
        performance.setQualityScore(qualityAvg);
        performance.setDeliveryScore(deliveryAvg);
        performance.setServiceScore(serviceAvg);
        performance.setPriceScore(priceAvg);
        performance.setEvaluationCount(evaluations.size());
        performance.setSuggestedLevel(SupplierScoreCalculator.levelFor(overall));
        performance.setRecentEvaluations(evaluations.stream().limit(5).toList());
        if (!evaluations.isEmpty()) {
            SupSupplierEvaluation latest = evaluations.get(0);
            performance.setLatestPeriod(latest.getEvaluationPeriod());
            performance.setLatestTotalScore(latest.getTotalScore());
            performance.setLatestEvaluatedAt(latest.getEvaluatedAt());
        }
        return performance;
    }

    @Override
    public List<SupSupplierChangeLog> listChangeLogs(String id, int limit) {
        requireSupplier(id);
        return changeLogService.listBySupplierId(id, limit);
    }

    @Override
    public List<SupSupplierCategory> getCategories(String id) {
        requireSupplier(id);
        List<String> categoryIds = categoryRelMapper.selectList(
                        Wrappers.<SupSupplierCategoryRel>lambdaQuery()
                                .eq(SupSupplierCategoryRel::getSupplierId, id))
                .stream()
                .map(SupSupplierCategoryRel::getCategoryId)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return categoryService.listByIds(categoryIds).stream()
                .filter(category -> !Boolean.TRUE.equals(category.getDeleted()))
                .sorted((left, right) -> {
                    int levelCompare = Integer.compare(
                            left.getLevel() == null ? 1 : left.getLevel(),
                            right.getLevel() == null ? 1 : right.getLevel());
                    if (levelCompare != 0) {
                        return levelCompare;
                    }
                    int sortCompare = Integer.compare(
                            left.getSortOrder() == null ? 0 : left.getSortOrder(),
                            right.getSortOrder() == null ? 0 : right.getSortOrder());
                    return sortCompare != 0 ? sortCompare
                            : String.valueOf(left.getCategoryName()).compareTo(String.valueOf(right.getCategoryName()));
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignCategories(String id, List<String> categoryIds, String operatorId, String operatorName) {
        requireSupplier(id);
        List<String> normalizedIds = categoryIds == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(categoryIds.stream()
                .filter(StringUtils::hasText)
                .toList()));

        List<SupSupplierCategory> categories = List.of();
        if (!normalizedIds.isEmpty()) {
            categories = categoryService.listByIds(normalizedIds);
            List<String> foundIds = categories.stream()
                    .filter(category -> !Boolean.TRUE.equals(category.getDeleted()))
                    .map(SupSupplierCategory::getId)
                    .toList();
            if (foundIds.size() != normalizedIds.size()) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "存在无效的供应商分类");
            }
        }

        categoryRelMapper.delete(Wrappers.<SupSupplierCategoryRel>lambdaQuery()
                .eq(SupSupplierCategoryRel::getSupplierId, id));
        for (String categoryId : normalizedIds) {
            SupSupplierCategoryRel relation = new SupSupplierCategoryRel()
                    .setId(UUID.randomUUID().toString())
                    .setSupplierId(id)
                    .setCategoryId(categoryId)
                    .setCreateTime(LocalDateTime.now());
            categoryRelMapper.insert(relation);
        }

        String names = categories.stream()
                .map(SupSupplierCategory::getCategoryName)
                .collect(Collectors.joining(", "));
        changeLogService.record(id, "CATEGORY_ASSIGN",
                "更新供应商分类: " + (names.isEmpty() ? "无" : names), null, operatorId, operatorName);
    }

    private SupSupplier requireSupplier(String id) {
        if (!StringUtils.hasText(id)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商ID不能为空");
        }
        SupSupplier supplier = getById(id);
        if (supplier == null || Boolean.TRUE.equals(supplier.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "供应商不存在");
        }
        return supplier;
    }

    private void validateMasterData(SupplierUpsertRequest request) {
        if (!StringUtils.hasText(request.getSupplierName())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商名称不能为空");
        }
        if (request.getSupplierName().length() > 256) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商名称长度不能超过 256");
        }
        if (request.getSupplierType() != null
                && (request.getSupplierType() < 1 || request.getSupplierType() > 4)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商类型仅支持 1-4");
        }
        if (StringUtils.hasText(request.getCreditRating())
                && !CREDIT_RATINGS.contains(request.getCreditRating())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST,
                    "信用评级仅支持 A+/A/B+/B/C/D");
        }
        if (request.getRegisteredCapital() != null
                && request.getRegisteredCapital().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "注册资本不能为负数");
        }
        if (StringUtils.hasText(request.getContactEmail()) && !request.getContactEmail().contains("@")) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "联系邮箱格式不正确");
        }
        if (StringUtils.hasText(request.getContactPhone()) && request.getContactPhone().length() > 32) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "联系电话长度不能超过 32");
        }
        if (StringUtils.hasText(request.getWebsite()) && request.getWebsite().length() > 256) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "官网地址长度不能超过 256");
        }
        normalizeCode(request.getSupplierCode());
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST,
                    "供应商编码仅支持字母、数字、下划线和短横线，长度 2-64");
        }
        return normalized;
    }

    private String normalizeTaxNo(String taxNo) {
        return StringUtils.hasText(taxNo) ? taxNo.trim().toUpperCase(Locale.ROOT) : null;
    }

    private void ensureCodeUnique(String code, String excludeId) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        long count = lambdaQuery()
                .eq(SupSupplier::getSupplierCode, code)
                .ne(StringUtils.hasText(excludeId), SupSupplier::getId, excludeId)
                .count();
        if (count > 0) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商编码已存在");
        }
    }

    private void ensureTaxNoUnique(String taxNo, String excludeId) {
        if (!StringUtils.hasText(taxNo)) {
            return;
        }
        long count = lambdaQuery()
                .eq(SupSupplier::getTaxNo, taxNo)
                .ne(StringUtils.hasText(excludeId), SupSupplier::getId, excludeId)
                .count();
        if (count > 0) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "税号已被其他供应商使用");
        }
    }

    private String generateSupplierCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "SUP-" + datePart + "-";
        String tenant = TenantContextHolder.getTenantId() == null
                ? "global" : TenantContextHolder.getTenantId().toString();
        String redisKey = "supplier:code:" + tenant + ":" + datePart;
        try {
            Long sequence = redisTemplate.opsForValue().increment(redisKey);
            if (sequence != null) {
                if (sequence == 1L) {
                    redisTemplate.expire(redisKey, Duration.ofDays(30));
                }
                return prefix + String.format("%04d", sequence);
            }
        } catch (Exception e) {
            log.warn("Redis 生成供应商编码失败，回退数据库计数: {}", e.getMessage());
        }
        long count = lambdaQuery().likeRight(SupSupplier::getSupplierCode, prefix).count();
        return prefix + String.format("%04d", count + 1);
    }

    private String describeChanges(SupSupplier before, SupSupplier after) {
        List<String> changes = new ArrayList<>();
        appendChange(changes, "供应商名称", before.getSupplierName(), after.getSupplierName());
        appendChange(changes, "英文名称", before.getSupplierNameEn(), after.getSupplierNameEn());
        appendChange(changes, "供应商类型", before.getSupplierType(), after.getSupplierType());
        appendChange(changes, "法人", before.getLegalPerson(), after.getLegalPerson());
        appendChange(changes, "税号", before.getTaxNo(), after.getTaxNo());
        appendChange(changes, "联系人", before.getContactName(), after.getContactName());
        appendChange(changes, "联系电话", before.getContactPhone(), after.getContactPhone());
        appendChange(changes, "信用评级", before.getCreditRating(), after.getCreditRating());
        appendChange(changes, "付款条件", before.getPaymentTerms(), after.getPaymentTerms());
        appendChange(changes, "付款方式", before.getPaymentMethod(), after.getPaymentMethod());
        appendChange(changes, "官网", before.getWebsite(), after.getWebsite());
        return changes.isEmpty() ? "供应商主数据更新" : String.join("；", changes);
    }

    private void appendChange(List<String> changes, String label, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(label + ": " + display(before) + " -> " + display(after));
        }
    }

    private String display(Object value) {
        return value == null ? "空" : String.valueOf(value);
    }
}
