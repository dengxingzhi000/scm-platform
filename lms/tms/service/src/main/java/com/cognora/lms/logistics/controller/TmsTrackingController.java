package com.cognora.lms.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.lms.logistics.support.webhook.WebhookSignatureVerifier;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.cognora.lms.logistics.domain.entity.TmsTracking;
import com.cognora.lms.logistics.service.ITmsTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-tracking")
@Tag(name = "物流跟踪", description = "运单物流轨迹的查询与轨迹节点上报接口。")
public class TmsTrackingController {

    private final ITmsTrackingService trackingService;
    private final WebhookSignatureVerifier webhookSignatureVerifier;
    private final ObjectMapper objectMapper;

    @Operation(summary = "查询轨迹详情", description = "根据轨迹主键 ID 获取单条物流轨迹记录。")
    @GetMapping("/{id}")
    public ApiResponse<TmsTracking> getById(
            @Parameter(description = "轨迹记录主键 ID") @PathVariable String id) {
        TmsTracking tracking = trackingService.getById(id);
        return ApiResponse.success(tracking);
    }

    @Operation(summary = "按运单查询轨迹", description = "根据运单主键 ID 获取该运单的全部物流轨迹。")
    @GetMapping("/waybill/{waybillId}")
    public ApiResponse<List<TmsTracking>> listByWaybillId(
            @Parameter(description = "运单主键 ID") @PathVariable String waybillId) {
        List<TmsTracking> list = trackingService.listByWaybillId(waybillId);
        return ApiResponse.success(list);
    }

    @Operation(summary = "按运单号查询轨迹", description = "根据运单号获取该运单的全部物流轨迹。")
    @GetMapping("/waybill-no/{waybillNo}")
    public ApiResponse<List<TmsTracking>> listByWaybillNo(
            @Parameter(description = "运单号") @PathVariable String waybillNo) {
        List<TmsTracking> list = trackingService.listByWaybillNo(waybillNo);
        return ApiResponse.success(list);
    }

    @Operation(summary = "分页查询轨迹", description = "按运单号和轨迹状态分页查询物流轨迹记录。")
    @GetMapping("/list")
    public ApiResponse<Page<TmsTracking>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String trackStatus) {
        Page<TmsTracking> result = trackingService.pageList(page, size, waybillNo, trackStatus);
        return ApiResponse.success(result);
    }

    @Operation(summary = "新增物流轨迹", description = "上报一条新的运单物流轨迹节点并返回创建结果。")
    @PostMapping
    @AuditLog(operation = "上报物流轨迹", businessType = "LOGISTICS", riskLevel = 2)
    public ApiResponse<TmsTracking> addTracking(@RequestBody TmsTracking tracking) {
        log.info("Add tracking: waybillNo={}, location={}, status={}", (tracking.getWaybillNo() + "").replace("\r", "_").replace("\n", "_"), (tracking.getLocation() + "").replace("\r", "_").replace("\n", "_"), (tracking.getTrackStatus() + "").replace("\r", "_").replace("\n", "_"));
        TmsTracking created = trackingService.addTracking(tracking);
        return ApiResponse.success(created);
    }

    @Operation(summary = "承运商轨迹回调（Webhook）",
            description = "验签通过后解析轨迹并幂等落库；验签失败返回 401 且不落库。"
                    + "请求头：X-Lms-Timestamp / X-Lms-Nonce / X-Lms-Signature。")
    @PostMapping("/callback/{carrierCode}")
    public ApiResponse<TmsTracking> callback(
            @Parameter(description = "承运商编码，用于回调来源审计") @PathVariable String carrierCode,
            @RequestHeader(value = "X-Lms-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lms-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Lms-Signature", required = false) String signature,
            @RequestBody String rawBody) throws JsonProcessingException {
        // 先验签，失败直接抛 401，后续解析与落库均不会执行。
        webhookSignatureVerifier.verifyAndConsume(timestamp, nonce, signature, rawBody);
        TmsTracking tracking = objectMapper.readValue(rawBody, TmsTracking.class);
        log.info("Carrier webhook verified: carrierCode={}, waybillNo={}, eventId={}",
                carrierCode, (tracking.getWaybillNo() + "").replace("\r", "_").replace("\n", "_"),
                (tracking.getEventId() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(trackingService.addTracking(tracking));
    }
}
