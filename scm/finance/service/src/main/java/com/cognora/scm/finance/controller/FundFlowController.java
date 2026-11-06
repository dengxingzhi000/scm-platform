package com.cognora.scm.finance.controller;

import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.finance.domain.entity.FundFlow;
import com.cognora.scm.finance.service.FundFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/fund-flow")
@Tag(name = "资金流水", description = "登记收款/付款流水；必须关联当前租户的结算单或发票。")
public class FundFlowController {

    private final FundFlowService fundFlowService;

    @Operation(summary = "登记资金流水", description = "幂等登记一条资金流水，孤儿流水与跨租户关联一律拒绝。")
    @PostMapping
    @AuditLog(operation = "登记资金流水", businessType = "FINANCE_FUND_FLOW", riskLevel = 4)
    public ApiResponse<FundFlow> record(@RequestBody FundFlow flow) {
        return ApiResponse.success(fundFlowService.record(flow));
    }
}
