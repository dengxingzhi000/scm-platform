package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurRequest;
import com.scmcloud.purchase.service.IPurRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-request")
@Tag(name = "采购申请", description = "采购申请单的查询、提交、审批与关闭接口")
public class PurRequestController {

    private final IPurRequestService purRequestService;

    @Operation(summary = "根据 ID 查询采购申请", description = "按主键查询单张采购申请单的完整信息。")
    @GetMapping("/{id}")
    public PurRequest getById(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return purRequestService.getById(id);
    }

    @Operation(summary = "根据申请单号查询", description = "按业务单号查询采购申请单，用于单据核对与追溯。")
    @GetMapping("/no/{requestNo}")
    public PurRequest getByRequestNo(@Parameter(description = "采购申请单业务单号") @PathVariable String requestNo) {
        return purRequestService.getByRequestNo(requestNo);
    }

    @Operation(summary = "分页查询采购申请", description = "按状态、申请类型和关键字分页查询采购申请单。")
    @GetMapping("/page")
    public Page<PurRequest> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "申请单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "申请类型") @RequestParam(required = false) Integer requestType,
            @Parameter(description = "关键字，用于模糊匹配单号等字段") @RequestParam(required = false) String keyword) {
        return purRequestService.pageQuery(page, size, status, requestType, keyword);
    }

    @Operation(summary = "按状态查询采购申请", description = "查询指定状态下的全部采购申请单，用于待办列表展示。")
    @GetMapping("/list")
    public List<PurRequest> listByStatus(@Parameter(description = "申请单状态") @RequestParam Integer status) {
        return purRequestService.listByStatus(status);
    }

    @Operation(summary = "新增采购申请", description = "创建一张新的采购申请单。")
    @PostMapping
    public boolean save(@RequestBody PurRequest purRequest) {
        return purRequestService.save(purRequest);
    }

    @Operation(summary = "修改采购申请", description = "按主键更新已有的采购申请单。")
    @PutMapping
    public boolean update(@RequestBody PurRequest purRequest) {
        return purRequestService.updateById(purRequest);
    }

    @Operation(summary = "删除采购申请", description = "按主键删除采购申请单。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return purRequestService.removeById(id);
    }

    @Operation(summary = "提交采购申请", description = "将草稿状态的采购申请单提交进入审批流程。")
    @PostMapping("/{id}/submit")
    public boolean submit(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return purRequestService.submit(id);
    }

    @Operation(summary = "审批通过采购申请", description = "记录审批人信息，将采购申请单置为审批通过状态。")
    @PostMapping("/{id}/approve")
    public boolean approve(
            @Parameter(description = "采购申请单主键 ID") @PathVariable String id,
            @Parameter(description = "审批人用户 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName) {
        return purRequestService.approve(id, approverId, approverName);
    }

    @Operation(summary = "驳回采购申请", description = "记录审批人信息与驳回原因，将采购申请单置为驳回状态。")
    @PostMapping("/{id}/reject")
    public boolean reject(
            @Parameter(description = "采购申请单主键 ID") @PathVariable String id,
            @Parameter(description = "审批人用户 ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName,
            @Parameter(description = "驳回原因") @RequestParam String reason) {
        return purRequestService.reject(id, approverId, approverName, reason);
    }

    @Operation(summary = "关闭采购申请", description = "关闭采购申请单，使其不再参与后续采购执行。")
    @PostMapping("/{id}/close")
    public boolean close(@Parameter(description = "采购申请单主键 ID") @PathVariable String id) {
        return purRequestService.close(id);
    }
}
