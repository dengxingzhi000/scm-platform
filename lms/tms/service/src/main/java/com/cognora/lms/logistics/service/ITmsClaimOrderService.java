package com.cognora.lms.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.lms.logistics.domain.entity.TmsClaimOrder;

import java.math.BigDecimal;

/**
 * 理赔单据状态流转（批次 3）。
 */
public interface ITmsClaimOrderService extends IService<TmsClaimOrder> {

    Page<TmsClaimOrder> pageList(int page, int size, String waybillNo, String status);

    TmsClaimOrder createClaim(String caseId, BigDecimal amount, String reason, String evidenceUrl, String operator);

    TmsClaimOrder transition(String id, String targetStatus, String operator, String remark);
}
