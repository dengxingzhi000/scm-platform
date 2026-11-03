package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;
import com.scmcloud.supplier.domain.support.SupplierScoreCalculator;
import com.scmcloud.supplier.mapper.SupSupplierEvaluationMapper;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import com.scmcloud.supplier.service.ISupSupplierEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierEvaluationServiceImpl
        extends ServiceImpl<SupSupplierEvaluationMapper, SupSupplierEvaluation>
        implements ISupSupplierEvaluationService {

    private final SupSupplierMapper supplierMapper;

    @Override
    public Page<SupSupplierEvaluation> pageList(int page, int size, String supplierId,
                                                String evaluationPeriod) {
        log.debug("分页查询供应商评价 page={}, size={}, supplierId={}", page, size, supplierId);

        LambdaQueryWrapper<SupSupplierEvaluation> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(SupSupplierEvaluation::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(evaluationPeriod)) {
            wrapper.eq(SupSupplierEvaluation::getEvaluationPeriod, evaluationPeriod);
        }

        wrapper.orderByDesc(SupSupplierEvaluation::getEvaluatedAt);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<SupSupplierEvaluation> listBySupplierId(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return List.of();
        }
        return lambdaQuery()
                .eq(SupSupplierEvaluation::getSupplierId, supplierId)
                .orderByDesc(SupSupplierEvaluation::getEvaluatedAt)
                .list();
    }

    @Override
    public BigDecimal calculateAverageScore(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> totalScores = lambdaQuery()
                .eq(SupSupplierEvaluation::getSupplierId, supplierId)
                .select(SupSupplierEvaluation::getTotalScore)
                .list()
                .stream()
                .map(SupSupplierEvaluation::getTotalScore)
                .toList();
        BigDecimal average = SupplierScoreCalculator.average(totalScores);
        return average == null ? BigDecimal.ZERO : average;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierEvaluation createEvaluation(SupSupplierEvaluation evaluation) {
        requireSupplier(evaluation.getSupplierId());
        if (!StringUtils.hasText(evaluation.getEvaluationPeriod())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "评价周期不能为空");
        }
        validateScores(evaluation);
        ensurePeriodUnique(evaluation.getSupplierId(), evaluation.getEvaluationPeriod(), null);

        fillPeriodDates(evaluation);
        if (evaluation.getEvaluatedAt() == null) {
            evaluation.setEvaluatedAt(LocalDateTime.now());
        }
        evaluation.setTotalScore(calculateTotal(evaluation));
        evaluation.setId(UUID.randomUUID().toString());
        evaluation.setTenantId(null);
        evaluation.setCreateBy(null);
        evaluation.setUpdateBy(null);
        evaluation.setCreateTime(LocalDateTime.now());
        evaluation.setUpdateTime(LocalDateTime.now());
        save(evaluation);
        refreshSupplierScorecard(evaluation.getSupplierId());
        log.info("创建供应商评价 id={}, supplierId={}, period={}",
                evaluation.getId(), evaluation.getSupplierId(), evaluation.getEvaluationPeriod());
        return evaluation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierEvaluation updateEvaluation(String id, SupSupplierEvaluation evaluation) {
        SupSupplierEvaluation existing = getById(id);
        if (existing == null) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "评价不存在");
        }
        validateScores(evaluation);
        String period = StringUtils.hasText(evaluation.getEvaluationPeriod())
                ? evaluation.getEvaluationPeriod() : existing.getEvaluationPeriod();
        ensurePeriodUnique(existing.getSupplierId(), period, id);

        BigDecimal quality = evaluation.getQualityScore() != null
                ? evaluation.getQualityScore() : existing.getQualityScore();
        BigDecimal delivery = evaluation.getDeliveryScore() != null
                ? evaluation.getDeliveryScore() : existing.getDeliveryScore();
        BigDecimal service = evaluation.getServiceScore() != null
                ? evaluation.getServiceScore() : existing.getServiceScore();
        BigDecimal price = evaluation.getPriceScore() != null
                ? evaluation.getPriceScore() : existing.getPriceScore();

        BigDecimal total;
        if (quality != null || delivery != null || service != null || price != null) {
            total = SupplierScoreCalculator.weightedTotal(quality, delivery, service, price);
        } else {
            total = evaluation.getTotalScore() != null
                    ? SupplierScoreCalculator.clamp(evaluation.getTotalScore())
                    : existing.getTotalScore();
        }

        evaluation.setId(id);
        evaluation.setSupplierId(existing.getSupplierId());
        evaluation.setEvaluationPeriod(period);
        evaluation.setTotalScore(total);
        evaluation.setTenantId(null);
        evaluation.setCreateBy(null);
        evaluation.setCreateTime(null);
        evaluation.setUpdateBy(null);
        evaluation.setUpdateTime(LocalDateTime.now());
        updateById(evaluation);
        refreshSupplierScorecard(existing.getSupplierId());
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEvaluation(String id) {
        SupSupplierEvaluation existing = getById(id);
        if (existing == null) {
            return false;
        }
        removeById(id);
        refreshSupplierScorecard(existing.getSupplierId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshSupplierScorecard(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return;
        }
        List<SupSupplierEvaluation> evaluations = listBySupplierId(supplierId);

        BigDecimal quality = SupplierScoreCalculator.average(evaluations.stream()
                .map(SupSupplierEvaluation::getQualityScore).toList());
        BigDecimal delivery = SupplierScoreCalculator.average(evaluations.stream()
                .map(SupSupplierEvaluation::getDeliveryScore).toList());
        BigDecimal service = SupplierScoreCalculator.average(evaluations.stream()
                .map(SupSupplierEvaluation::getServiceScore).toList());
        BigDecimal price = SupplierScoreCalculator.average(evaluations.stream()
                .map(SupSupplierEvaluation::getPriceScore).toList());
        BigDecimal overall = SupplierScoreCalculator.weightedTotal(quality, delivery, service, price);

        LocalDateTime lastEvaluatedAt = evaluations.stream()
                .map(SupSupplierEvaluation::getEvaluatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        LambdaUpdateWrapper<SupSupplier> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SupSupplier::getId, supplierId);
        wrapper.set(SupSupplier::getQualityScore, quality);
        wrapper.set(SupSupplier::getDeliveryScore, delivery);
        wrapper.set(SupSupplier::getServiceScore, service);
        wrapper.set(SupSupplier::getOverallScore, overall);
        wrapper.set(SupSupplier::getSupplierLevel, SupplierScoreCalculator.levelFor(overall));
        wrapper.set(SupSupplier::getLastEvaluationAt, lastEvaluatedAt);
        wrapper.set(SupSupplier::getUpdateTime, LocalDateTime.now());
        supplierMapper.update(null, wrapper);
        log.info("回写供应商绩效 supplierId={}, overall={}", supplierId, overall);
    }

    private BigDecimal calculateTotal(SupSupplierEvaluation evaluation) {
        if (evaluation.getQualityScore() == null && evaluation.getDeliveryScore() == null
                && evaluation.getServiceScore() == null && evaluation.getPriceScore() == null) {
            return evaluation.getTotalScore() == null ? BigDecimal.ZERO
                    : SupplierScoreCalculator.clamp(evaluation.getTotalScore());
        }
        return SupplierScoreCalculator.weightedTotal(evaluation);
    }

    private void fillPeriodDates(SupSupplierEvaluation evaluation) {
        LocalDate start = evaluation.getStartDate();
        LocalDate end = evaluation.getEndDate();
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                throw new ServiceException(400, HttpStatus.BAD_REQUEST, "评价结束日期不能早于开始日期");
            }
            return;
        }
        LocalDate reference = end != null ? end : (start != null ? start : LocalDate.now());
        LocalDate periodStart = resolvePeriodStart(evaluation.getEvaluationPeriod(), reference);
        LocalDate periodEnd = periodStart.with(TemporalAdjusters.lastDayOfMonth());
        if (start == null) {
            evaluation.setStartDate(periodStart);
        }
        if (end == null) {
            evaluation.setEndDate(periodEnd.isBefore(start == null ? periodStart : start) ? start : periodEnd);
        }
    }

    private LocalDate resolvePeriodStart(String period, LocalDate reference) {
        if (StringUtils.hasText(period)) {
            try {
                if (period.matches("\\d{4}-\\d{2}")) {
                    return LocalDate.parse(period + "-01");
                }
                if (period.matches("\\d{4}Q[1-4]")) {
                    int year = Integer.parseInt(period.substring(0, 4));
                    int quarter = Integer.parseInt(period.substring(5));
                    return LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
                }
            } catch (RuntimeException e) {
                log.warn("无法解析评价周期: {}", period);
            }
        }
        return reference.withDayOfMonth(1);
    }

    private void validateScores(SupSupplierEvaluation evaluation) {
        checkScore("质量得分", evaluation.getQualityScore());
        checkScore("交付得分", evaluation.getDeliveryScore());
        checkScore("服务得分", evaluation.getServiceScore());
        checkScore("价格得分", evaluation.getPriceScore());
    }

    private void checkScore(String label, BigDecimal score) {
        if (score != null && (score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(new BigDecimal("100")) > 0)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, label + "必须在 0-100 之间");
        }
    }

    private void ensurePeriodUnique(String supplierId, String period, String excludeId) {
        long count = lambdaQuery()
                .eq(SupSupplierEvaluation::getSupplierId, supplierId)
                .eq(SupSupplierEvaluation::getEvaluationPeriod, period)
                .ne(StringUtils.hasText(excludeId), SupSupplierEvaluation::getId, excludeId)
                .count();
        if (count > 0) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "该供应商此评价周期已存在评价记录");
        }
    }

    private SupSupplier requireSupplier(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "供应商ID不能为空");
        }
        SupSupplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || Boolean.TRUE.equals(supplier.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "供应商不存在");
        }
        return supplier;
    }
}
