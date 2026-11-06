package com.cognora.scm.finance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账差异明细。每条差异独立闭环，不再依赖 reconciliation_record.status 一个字段兜底。
 *
 * <p>状态：0-待处理，1-已确认，2-已核销，3-已驳回；合法流转见
 * {@link com.cognora.scm.finance.domain.enums.ReconciliationDiffStatus}。</p>
 */
@Data
@TableName("fin_reconciliation_diff")
public class ReconciliationDiff {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    private String reconciliationId;

    private String reconciliationNo;

    /** AMOUNT / MISSING_DOC / DUPLICATE / OTHER */
    private String diffType;

    private BigDecimal ourAmount;

    private BigDecimal theirAmount;

    private BigDecimal diffAmount;

    private String currency;

    /** 0-待处理, 1-已确认, 2-已核销, 3-已驳回 */
    private Integer status;

    private String resolution;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;
}
