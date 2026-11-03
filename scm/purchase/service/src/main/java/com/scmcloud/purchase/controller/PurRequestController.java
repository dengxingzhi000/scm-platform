package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.purchase.domain.dto.PurRequestConvertDTO;
import com.scmcloud.purchase.domain.dto.PurRequestDetailDTO;
import com.scmcloud.purchase.domain.dto.PurRequestUpsertDTO;
import com.scmcloud.purchase.domain.entity.PurOrder;
import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.service.IPurRequestItemService;
import com.scmcloud.purchase.service.IPurRequestService;
import com.scmcloud.purchase.service.command.PurRequestCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-request")
@Tag(name = "采购申请", description = "采购申请的草稿维护、提交、审批与转采购单。")
public class PurRequestController {

    private final IPurRequestService purRequestService;
    private final IPurRequestItemService purRequestItemService;
    private final PurRequestCommandService purRequestCommandService;

    @Operation(summary = "根据 ID 查询采购申请", description = "按主键查询单张采购申请单的完整信息。")
    @GetMapping("/{id}")
    public ApiResponse<PurRequest> getById(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return ApiResponse.success(requireRequest(id));
    }

    @Operation(summary = "查询采购申请及明细", description = "一次返回申请单头和全部明细行，供详情与转单使用。")
    @GetMapping("/{id}/detail")
    public ApiResponse<PurRequestDetailDTO> detail(
            @Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        PurRequest request = requireRequest(id);
        return ApiResponse.success(new PurRequestDetailDTO(
                request, purRequestItemService.listByRequestId(request.getId())));
    }

    @Operation(summary = "根据申请单号查询", description = "按业务单号查询采购申请单，用于单据核对与追溯。")
    @GetMapping("/no/{requestNo}")
    public ApiResponse<PurRequest> getByRequestNo(
            @Parameter(description = "采购申请单业务单号") @PathVariable String requestNo) {
        PurRequest request = purRequestService.getByRequestNo(requestNo);
        if (request == null) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "采购申请不存在: " + requestNo);
        }
        return ApiResponse.success(request);
    }

    @Operation(summary = "分页查询采购申请", description = "按状态、申请类型和关键字分页查询采购申请单。")
    @GetMapping("/page")
    public ApiResponse<Page<PurRequest>> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "申请单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "申请类型") @RequestParam(required = false) Integer requestType,
            @Parameter(description = "关键字，用于模糊匹配单号、申请人与用途") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(purRequestService.pageQuery(page, size, status, requestType, keyword));
    }

    @Operation(summary = "按状态查询采购申请", description = "查询指定状态下的全部采购申请单，用于待办列表展示。")
    @GetMapping("/list")
    public ApiResponse<List<PurRequest>> listByStatus(
            @Parameter(description = "申请单状态") @RequestParam Integer status) {
        return ApiResponse.success(purRequestService.listByStatus(status));
    }

    @Operation(summary = "创建采购申请草稿", description = "在同一事务内创建申请单头与全部明细，单号与申请人由服务端补齐。")
    @PostMapping("/full")
    public ApiResponse<PurRequest> create(
            @Parameter(description = "申请单头与明细行") @RequestBody PurRequestUpsertDTO payload) {
        PurRequestUpsertDTO body = requirePayload(payload);
        Operator operator = currentOperator();
        return ApiResponse.success(purRequestCommandService.createDraft(
                body.request(), body.items(), operator.id(), operator.name()));
    }

    @Operation(summary = "更新采购申请草稿", description = "仅草稿或已驳回的申请可编辑，明细整体替换。")
    @PutMapping("/{id}/full")
    public ApiResponse<PurRequest> update(
            @Parameter(description = "采购申请单主键 ID") @PathVariable String id,
            @Parameter(description = "申请单头与明细行") @RequestBody PurRequestUpsertDTO payload) {
        PurRequestUpsertDTO body = requirePayload(payload);
        return ApiResponse.success(purRequestCommandService.updateDraft(
                id, body.request(), body.items()));
    }

    @Operation(summary = "删除采购申请", description = "仅草稿或已驳回的申请可以删除，明细一并清理。")
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return ApiResponse.success(purRequestCommandService.delete(id));
    }

    @Operation(summary = "提交采购申请", description = "将草稿或已驳回的申请提交进入审批流程。")
    @PostMapping("/{id}/submit")
    public ApiResponse<Boolean> submit(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return ApiResponse.success(purRequestCommandService.submit(id));
    }

    @Operation(summary = "审批通过采购申请", description = "审批人取自登录态，将申请单置为已审批。")
    @PostMapping("/{id}/approve")
    public ApiResponse<Boolean> approve(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        Operator operator = currentOperator();
        return ApiResponse.success(purRequestCommandService.approve(id, operator.id(), operator.name()));
    }

    @Operation(summary = "驳回采购申请", description = "审批人取自登录态，驳回原因必填。")
    @PostMapping("/{id}/reject")
    public ApiResponse<Boolean> reject(
            @Parameter(description = "采购申请单主键 ID") @PathVariable String id,
            @Parameter(description = "驳回原因") @RequestParam String reason) {
        Operator operator = currentOperator();
        return ApiResponse.success(
                purRequestCommandService.reject(id, operator.id(), operator.name(), reason));
    }

    @Operation(summary = "关闭采购申请", description = "关闭未转单的采购申请，已转单的申请不能关闭。")
    @PostMapping("/{id}/close")
    public ApiResponse<Boolean> close(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return ApiResponse.success(purRequestCommandService.close(id));
    }

    @Operation(summary = "采购申请转采购单", description = "原子生成采购订单与明细，并回写申请单的转单状态。")
    @PostMapping("/{id}/convert")
    public ApiResponse<PurOrder> convert(
            @Parameter(description = "采购申请单主键 ID") @PathVariable String id,
            @Parameter(description = "供应商、仓库、价格与到货信息") @RequestBody PurRequestConvertDTO payload) {
        PurRequestConvertDTO body = requirePayload(payload);
        Operator operator = currentOperator();
        return ApiResponse.success(purRequestCommandService.convert(
                id, body, operator.id(), operator.name()));
    }

    private PurRequest requireRequest(String id) {
        PurRequest request = purRequestService.getById(id);
        if (request == null || Boolean.TRUE.equals(request.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "采购申请不存在: " + id);
        }
        return request;
    }

    private <T> T requirePayload(T payload) {
        if (payload == null) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "请求体不能为空");
        }
        return payload;
    }

    private Operator currentOperator() {
        UUID userId = SecurityUtils.getCurrentUserUuid()
                .orElseThrow(() -> new ServiceException(401, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"));
        SecurityUser user = SecurityUtils.getCurrentUser();
        String name = user != null && StringUtils.hasText(user.getRealName())
                ? user.getRealName()
                : SecurityUtils.getCurrentUsername().orElse(userId.toString());
        return new Operator(userId.toString(), name);
    }

    private record Operator(String id, String name) {
    }
}
