package com.cognora.scm.finance.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.scm.finance.domain.entity.FundFlow;
import com.cognora.scm.finance.domain.entity.Invoice;
import com.cognora.scm.finance.domain.entity.SettlementOrder;
import com.cognora.scm.finance.domain.support.FinanceAmount;
import com.cognora.scm.finance.mapper.FundFlowMapper;
import com.cognora.scm.finance.mapper.InvoiceMapper;
import com.cognora.scm.finance.mapper.SettlementOrderMapper;
import com.cognora.scm.finance.service.FundFlowService;
import com.cognora.scm.finance.service.support.AccountingPeriodGuard;
import com.cognora.scm.finance.service.support.FinanceTenantGuard;
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
 *
 * <p>为避免出现无法追溯的孤儿流水，每条流水必须挂到当前租户下的结算单或发票；
 * 关联规则见 {@link #validateLink(FundFlow)}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundFlowServiceImpl implements FundFlowService {

    private static final Set<String> DIRECTIONS = Set.of("IN", "OUT");
    private static final int MAX_KEY_LENGTH = 128;

    private static final String SETTLEMENT_PAYMENT = "SETTLEMENT_PAYMENT";
    private static final String INVOICE_PAYMENT = "INVOICE_PAYMENT";
    private static final String REFUND = "REFUND";
    private static final String ADJUSTMENT = "ADJUSTMENT";
    private static final Set<String> SOURCE_TYPES =
            Set.of(SETTLEMENT_PAYMENT, INVOICE_PAYMENT, REFUND, ADJUSTMENT);

    private final FundFlowMapper fundFlowMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final InvoiceMapper invoiceMapper;
    private final AccountingPeriodGuard accountingPeriodGuard;

    @Override
    public FundFlow record(FundFlow flow) {
        String idempotencyKey = flow.getIdempotencyKey();
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("幂等键必填且不超过" + MAX_KEY_LENGTH + "字符");
        }
        if (!DIRECTIONS.contains(flow.getDirection())) {
            throw new IllegalArgumentException("资金方向必须为 IN 或 OUT: " + flow.getDirection());
        }
        // 请求体显式携带其他租户的 tenantId 时直接 403。
        FinanceTenantGuard.verifyOwnership(flow.getTenantId());

        String sourceType = StringUtils.hasText(flow.getSourceType())
                ? flow.getSourceType().trim().toUpperCase() : null;
        validateLink(flow, sourceType);

        flow.setId(UUIDv7Util.generateString());
        flow.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        flow.setSourceType(sourceType);
        flow.setAmount(FinanceAmount.positive(flow.getAmount(), "流水金额"));
        flow.setCurrency(FinanceAmount.currency(flow.getCurrency()));
        flow.setIdempotencyKey(idempotencyKey.trim());
        if (!StringUtils.hasText(flow.getFlowNo())) {
            flow.setFlowNo("FF" + System.currentTimeMillis());
        }
        if (flow.getOccurTime() == null) {
            flow.setOccurTime(LocalDateTime.now());
        }
        accountingPeriodGuard.assertWritable(flow.getOccurTime().toLocalDate(), "资金流水:" + flow.getFlowNo());
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

    /**
     * 关联校验：禁止孤儿流水，并校验被关联单据存在且属于当前租户。
     */
    private void validateLink(FundFlow flow, String sourceType) {
        if (!StringUtils.hasText(sourceType) || !SOURCE_TYPES.contains(sourceType)) {
            throw new IllegalArgumentException(
                    "资金来源类型必须为 SETTLEMENT_PAYMENT/INVOICE_PAYMENT/REFUND/ADJUSTMENT: " + flow.getSourceType());
        }
        boolean hasSettlement = StringUtils.hasText(flow.getSettlementId());
        boolean hasInvoice = StringUtils.hasText(flow.getInvoiceId());
        if (!hasSettlement && !hasInvoice) {
            throw new IllegalArgumentException("孤儿流水被拒绝：必须关联结算单或发票");
        }
        if (SETTLEMENT_PAYMENT.equals(sourceType) && !hasSettlement) {
            throw new IllegalArgumentException("结算付款流水必须关联结算单");
        }
        if (INVOICE_PAYMENT.equals(sourceType) && !hasInvoice) {
            throw new IllegalArgumentException("发票付款流水必须关联发票");
        }

        if (hasSettlement) {
            SettlementOrder settlement = settlementOrderMapper.selectById(flow.getSettlementId());
            if (settlement == null || Boolean.TRUE.equals(settlement.getDeleted())) {
                throw new IllegalArgumentException("关联结算单不存在: " + flow.getSettlementId());
            }
            FinanceTenantGuard.verifyOwnership(settlement.getTenantId());
            accountingPeriodGuard.assertWritable(settlement.getSettlementPeriod(), settlement.getStartDate(),
                    "资金流水关联结算单:" + settlement.getSettlementNo());
            if (!StringUtils.hasText(flow.getSourceNo())) {
                flow.setSourceNo(settlement.getSettlementNo());
            }
        }
        if (hasInvoice) {
            Invoice invoice = invoiceMapper.selectById(flow.getInvoiceId());
            if (invoice == null || Boolean.TRUE.equals(invoice.getDeleted())) {
                throw new IllegalArgumentException("关联发票不存在: " + flow.getInvoiceId());
            }
            FinanceTenantGuard.verifyOwnership(invoice.getTenantId());
            accountingPeriodGuard.assertWritable(invoice.getInvoiceDate(), "资金流水关联发票:" + invoice.getInvoiceNo());
            if (!StringUtils.hasText(flow.getSourceNo())) {
                flow.setSourceNo(invoice.getInvoiceNo());
            }
        }
    }

    private FundFlow findByKey(String idempotencyKey) {
        return fundFlowMapper.selectOne(Wrappers.<FundFlow>lambdaQuery()
                .eq(FundFlow::getTenantId, TenantContextHolder.getRequiredTenantId().toString())
                .eq(FundFlow::getIdempotencyKey, idempotencyKey));
    }
}
