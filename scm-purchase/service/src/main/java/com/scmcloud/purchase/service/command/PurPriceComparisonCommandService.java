package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.decision.engine.ConstraintResult;
import com.scmcloud.decision.engine.ConstraintType;
import com.scmcloud.purchase.domain.entity.PurPriceComparison;
import com.scmcloud.purchase.domain.entity.PurPriceComparisonItem;
import com.scmcloud.purchase.engine.PriceComparisonEngine;
import com.scmcloud.purchase.engine.PriceComparisonInput;
import com.scmcloud.purchase.engine.PriceComparisonOutput;
import com.scmcloud.purchase.mapper.PurPriceComparisonItemMapper;
import com.scmcloud.purchase.mapper.PurPriceComparisonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.common.status.StatusValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPriceComparisonCommandService {

    private final PurPriceComparisonMapper purPriceComparisonMapper;
    private final PurPriceComparisonItemMapper purPriceComparisonItemMapper;
    private final StatusValidator statusValidator;
    private final PriceComparisonEngine priceComparisonEngine;
    private final ObjectMapper objectMapper;

    @Master(reason = "保存比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurPriceComparison entity) {
        return purPriceComparisonMapper.insert(entity) > 0;
    }

    @Master(reason = "更新比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurPriceComparison entity) {
        return purPriceComparisonMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purPriceComparisonMapper.deleteById(id) > 0;
    }

    @Master(reason = "审批比价分析")
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurPriceComparison comparison = purPriceComparisonMapper.selectById(id);
        if (comparison == null || comparison.getDeleted()) {
            throw new IllegalArgumentException("比价分析不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "PENDING_APPROVAL", "APPROVED");
        comparison.setStatus(2); // APPROVED
        comparison.setApprovedBy(approverId);
        comparison.setApprovedByName(approverName);
        comparison.setApprovedAt(LocalDateTime.now());
        comparison.setUpdateTime(LocalDateTime.now());
        return purPriceComparisonMapper.updateById(comparison) > 0;
    }

    @Master(reason = "执行比价评分算法")
    @Transactional(rollbackFor = Exception.class)
    public List<PriceComparisonOutput.SupplierRanking> executeScoring(String comparisonId) {
        log.info("执行比价评分算法: comparisonId={}", comparisonId);

        PurPriceComparison comparison = purPriceComparisonMapper.selectById(comparisonId);
        if (comparison == null || comparison.getDeleted()) {
            throw new IllegalArgumentException("比价分析不存在: " + comparisonId);
        }

        List<PurPriceComparisonItem> existingItems = purPriceComparisonItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurPriceComparisonItem>()
                        .eq(PurPriceComparisonItem::getComparisonId, comparisonId)
        );

        PriceComparisonInput input = buildScoringInput(comparisonId, existingItems);
        List<PriceComparisonOutput.SupplierRanking> rankings = priceComparisonEngine.rank(input);

        updateComparisonItems(comparisonId, rankings);

        log.info("比价评分完成: comparisonId={}, supplierCount={}", comparisonId, rankings.size());
        return rankings;
    }

    private PriceComparisonInput buildScoringInput(String comparisonId, List<PurPriceComparisonItem> items) {
        PriceComparisonInput input = new PriceComparisonInput();
        input.setComparisonId(comparisonId);

        List<PriceComparisonInput.SupplierQuote> quotes = items.stream()
                .map(item -> {
                    PriceComparisonInput.SupplierQuote quote = new PriceComparisonInput.SupplierQuote();
                    quote.setSupplierId(item.getSupplierId());
                    quote.setSupplierName(item.getSupplierName());
                    quote.setQuotationId(item.getQuotationId());
                    quote.setSupplierStatus("ACTIVE");
                    quote.setSupplierLevel("B");
                    quote.setQualityScore(80.0);
                    quote.setDeliveryScore(80.0);
                    quote.setServiceScore(80.0);
                    return quote;
                })
                .collect(Collectors.toList());

        input.setQuotes(quotes);
        return input;
    }

    private void updateComparisonItems(String comparisonId, List<PriceComparisonOutput.SupplierRanking> rankings) {

        for (int i = 0; i < rankings.size(); i++) {
            PriceComparisonOutput.SupplierRanking ranking = rankings.get(i);
            int rank = i + 1;

            PurPriceComparisonItem existingItem = purPriceComparisonItemMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurPriceComparisonItem>()
                            .eq(PurPriceComparisonItem::getComparisonId, comparisonId)
                            .eq(PurPriceComparisonItem::getSupplierId, ranking.getSupplierId())
            );

            if (existingItem != null) {
                try {
                    String scoresJson = objectMapper.writeValueAsString(ranking.getScores());
                    existingItem.setScores(scoresJson);
                } catch (Exception e) {
                    log.warn("Failed to serialize scores: {}", e.getMessage());
                    existingItem.setScores("{}");
                }
                existingItem.setTotalScore(new java.math.BigDecimal(ranking.getTotalScore()));
                existingItem.setRank(rank);
                purPriceComparisonItemMapper.updateById(existingItem);
            }
        }
    }
}
