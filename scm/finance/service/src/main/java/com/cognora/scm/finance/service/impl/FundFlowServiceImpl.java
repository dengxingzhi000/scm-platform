package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.FundFlow;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.FundFlowMapper;
import com.cognora.scm.finance.service.FundFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 资金流水登记。幂等性由数据库唯一约束 (tenant_id, idempotency_key) 兜底，
 * 应用层只负责把「重复提交」与「幂等键冲突」区分开。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundFlowServiceImpl implements FundFlowService {

    private static final Set<String> DIRECTIONS = Set.of("IN", "OUT");
    private static final int MAX_KEY_LENGTH = 128;

    private final FundFlowMapper fundFlowMapper;

    @Override
    public FundFlow record(FundFlow flow) {
        String idempotencyKey = flow.getIdempotencyKey();
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("幂等键必填且不超过" + MAX_KEY_LENGTH + "字符");
        }
        if (!DIRECTIONS.contains(flow.getDirection())) {
            throw new IllegalArgumentException("资金方向必须为 IN 或 OUT: " + flow.getDirection());
        }
        flow.setId(UUIDv7Util.generateString());
        flow.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        flow.setAmount(FinanceAmount.positive(flow.getAmount(), "流水金额"));
        flow.setCurrency(FinanceAmount.currency(flow.getCurrency()));
        flow.setIdempotencyKey(idempotencyKey.trim());
        if (!StringUtils.hasText(flow.getFlowNo())) {
            flow.setFlowNo("FF" + System.currentTimeMillis());
        }
        if (flow.getOccurTime() == null) {
            flow.setOccurTime(LocalDateTime.now());
        }
        flow.setCreateTime(LocalDateTime.now());

        try {
            fundFlowMapper.insert(flow);
            return flow;
        } catch (DuplicateKeyException duplicate) {
            FundFlow existing = findByKey(flow.getIdempotencyKey());
            if (existing == null) {
                throw duplicate;
            }
            if (existing.getAmount().compareTo(flow.getAmount()) != 0
                    || !existing.getDirection().equals(flow.getDirection())) {
                throw new IllegalStateException("幂等键已被不同金额或方向的流水占用: " + flow.getIdempotencyKey());
            }
            log.info("资金流水重复提交，按幂等返回既有记录: key={}", flow.getIdempotencyKey());
            return existing;
        }
    }

    private FundFlow findByKey(String idempotencyKey) {
        return fundFlowMapper.selectOne(Wrappers.<FundFlow>lambdaQuery()
                .eq(FundFlow::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(FundFlow::getIdempotencyKey, idempotencyKey));
    }
}
