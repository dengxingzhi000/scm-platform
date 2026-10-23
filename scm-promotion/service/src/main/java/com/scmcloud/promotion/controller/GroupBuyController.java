package com.scmcloud.promotion.controller;

import com.scmcloud.promotion.domain.entity.GroupBuy;
import com.scmcloud.promotion.domain.entity.GroupBuyMember;
import com.scmcloud.promotion.service.IGroupBuyService;
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
public class GroupBuyController {

    private final IGroupBuyService groupBuyService;

    @PostMapping
    public GroupBuy createGroup(@RequestBody CreateGroupRequest request) {
        log.info("[API] Create group buy: userId={}, activityId={}",
                request.getUserId(), request.getActivityId());
        return groupBuyService.createGroup(request.getUserId(), request.getActivityId(),
                request.getProductId(), request.getSkuId());
    }

    @PostMapping("/{groupBuyId}/join")
    public GroupBuy joinGroup(@PathVariable Long groupBuyId, @RequestBody JoinGroupRequest request) {
        log.info("[API] Join group buy: groupBuyId={}, userId={}", groupBuyId, request.getUserId());
        return groupBuyService.joinGroup(request.getUserId(), groupBuyId);
    }

    @GetMapping("/{groupBuyId}")
    public GroupBuy getGroup(@PathVariable Long groupBuyId) {
        return groupBuyService.getById(groupBuyId);
    }

    @GetMapping("/{groupBuyId}/members")
    public List<GroupBuyMember> listMembers(@PathVariable Long groupBuyId) {
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
