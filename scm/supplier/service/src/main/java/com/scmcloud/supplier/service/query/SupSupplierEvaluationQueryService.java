package com.scmcloud.supplier.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.supplier.domain.entity.SupSupplierEvaluation;
import com.scmcloud.supplier.mapper.SupSupplierEvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierEvaluationQueryService {

    private final SupSupplierEvaluationMapper supSupplierEvaluationMapper;

    @Slave
    public SupSupplierEvaluation getById(String id) {
        return supSupplierEvaluationMapper.selectById(id);
    }

    @Slave
    public List<SupSupplierEvaluation> listAll() {
        return supSupplierEvaluationMapper.selectList(null);
    }

    @Slave
    public Page<SupSupplierEvaluation> pageQuery(Page<SupSupplierEvaluation> page, Wrapper<SupSupplierEvaluation> wrapper) {
        return supSupplierEvaluationMapper.selectPage(page, wrapper);
    }

    @Slave
    public Page<SupSupplierEvaluation> pageList(int page, int size, String supplierId, String evaluationPeriod) {
        log.debug("分页查询供应商评价: page={}, size={}, supplierId={}", page, size, supplierId);
        LambdaQueryWrapper<SupSupplierEvaluation> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(SupSupplierEvaluation::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(evaluationPeriod)) {
            wrapper.eq(SupSupplierEvaluation::getEvaluationPeriod, evaluationPeriod);
        }
        wrapper.orderByDesc(SupSupplierEvaluation::getEvaluatedAt);
        return supSupplierEvaluationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<SupSupplierEvaluation> listBySupplierId(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return List.of();
        }
        log.debug("查询供应商的所有评价: supplierId={}", supplierId);
        LambdaQueryWrapper<SupSupplierEvaluation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupSupplierEvaluation::getSupplierId, supplierId);
        wrapper.orderByDesc(SupSupplierEvaluation::getEvaluatedAt);
        return supSupplierEvaluationMapper.selectList(wrapper);
    }

    @Slave
    public BigDecimal calculateAverageScore(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            return BigDecimal.ZERO;
        }
        log.debug("计算供应商平均评分: supplierId={}", supplierId);
        LambdaQueryWrapper<SupSupplierEvaluation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupSupplierEvaluation::getSupplierId, supplierId);
        wrapper.select(SupSupplierEvaluation::getTotalScore);
        List<SupSupplierEvaluation> evaluations = supSupplierEvaluationMapper.selectList(wrapper);
        if (evaluations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = evaluations.stream()
                .map(SupSupplierEvaluation::getTotalScore)
                .filter(score -> score != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = evaluations.stream()
                .filter(e -> e.getTotalScore() != null)
                .count();
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
