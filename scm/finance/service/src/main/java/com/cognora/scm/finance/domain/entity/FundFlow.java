package com.cognora.scm.finance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水（收款/付款凭证）。
 *
 * <p>{@code idempotency_key} 在租户内唯一，重复入账时由数据库唯一约束拒绝，
 * 而不是依赖应用层先查后写——后者在并发下必然漏。</p>
 */
@Data
@TableName("fin_fund_flow")
public class FundFlow {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String flowNo;

    /** IN / OUT */
    private String direction;

    private BigDecimal amount;

    private String currency;

    private String settlementId;

    private String invoiceId;

    private String sourceType;

    private String sourceNo;

    private String idempotencyKey;

    private String operatorId;

    private LocalDateTime occurTime;

    private LocalDateTime createTime;

    private String createBy;

    private String remark;
}
