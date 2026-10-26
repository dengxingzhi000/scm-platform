package com.scmcloud.promotion.controller;

import com.scmcloud.promotion.domain.entity.GroupBuy;
import com.scmcloud.promotion.domain.entity.GroupBuyMember;
import com.scmcloud.promotion.service.IGroupBuyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/promotions/groups")
@Tag(name = "拼团活动", description = "拼团团组的创建、参团、详情与成团成员查询接口。")
public class GroupBuyController {

    private final IGroupBuyService groupBuyService;

    @Operation(summary = "发起拼团", description = "为指定商品 SKU 创建新的拼团团组并返回团组信息。")
    @PostMapping
    public GroupBuy createGroup(@RequestBody CreateGroupRequest request) {
        log.info("[API] Create group buy: userId={}, activityId={}",
                request.getUserId(), request.getActivityId());
        return groupBuyService.createGroup(request.getUserId(), request.getActivityId(),
                request.getProductId(), request.getSkuId());
    }

    @Operation(summary = "参与拼团", description = "指定用户加入已存在的拼团团组并返回更新后的团组信息。")
    @PostMapping("/{groupBuyId}/join")
    public GroupBuy joinGroup(@Parameter(description = "拼团团组 ID") @PathVariable Long groupBuyId, @RequestBody JoinGroupRequest request) {
        log.info("[API] Join group buy: groupBuyId={}, userId={}", groupBuyId, request.getUserId());
        return groupBuyService.joinGroup(request.getUserId(), groupBuyId);
    }

    @Operation(summary = "查询拼团详情", description = "根据团组 ID 获取拼团团组的详细信息。")
    @GetMapping("/{groupBuyId}")
    public GroupBuy getGroup(@Parameter(description = "拼团团组 ID") @PathVariable Long groupBuyId) {
        return groupBuyService.getById(groupBuyId);
    }

    @Operation(summary = "查询拼团成员", description = "获取指定拼团团组的全部参团成员列表。")
    @GetMapping("/{groupBuyId}/members")
    public List<GroupBuyMember> listMembers(@Parameter(description = "拼团团组 ID") @PathVariable Long groupBuyId) {
        return groupBuyService.listMembers(groupBuyId);
    }

    @Data
    public static class CreateGroupRequest {
        private Long userId;
        private Long activityId;
        private Long productId;
        private Long skuId;
    }

    @Data
    public static class JoinGroupRequest {
        private Long userId;
    }
}
