package com.cognora.scm.finance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计期间（账期）。账期锁本身在批次 2 接入单据写入路径，
 * 本批次只落表结构，避免与状态机改造混在一起。
 */
@Data
@TableName("fin_accounting_period")
public class AccountingPeriod {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String tenantId;

    /** YYYY-MM */
    private String periodCode;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 0-未结账, 1-已结账, 2-已重开 */
    private Integer status;

    private String closedBy;

    private LocalDateTime closedAt;

    /** 重开操作人；仅在最近一次重开时写入，重开后再次关账也不清空。 */
    private String reopenedBy;

    /** 最近一次重开时间。 */
    private LocalDateTime reopenedAt;

    /** 最近一次重开原因，与操作人/时间一并留痕，作为审计补充。 */
    private String reopenReason;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    private Boolean deleted;

    private String remark;
}
