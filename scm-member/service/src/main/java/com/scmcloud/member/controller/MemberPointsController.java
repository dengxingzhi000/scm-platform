package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.MemberPointsLog;
import com.scmcloud.member.service.IMemberPointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members/{userId}/points")
@Tag(name = "会员积分", description = "会员积分余额查询、积分流水查询与积分增减管理")
public class MemberPointsController {
    private final IMemberPointsService pointsService;

    @Operation(summary = "查询积分余额", description = "查询指定会员当前可用的积分余额。")
    @GetMapping
    public int getBalance(@Parameter(description = "会员用户 ID") @PathVariable String userId) {
        log.info("[API] Get points balance: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        return pointsService.getPointsBalance(userId);
    }

    @Operation(summary = "查询积分明细", description = "查询指定会员的积分变动流水记录。")
    @GetMapping("/log")
    public List<MemberPointsLog> getLog(@Parameter(description = "会员用户 ID") @PathVariable String userId) {
        log.info("[API] Get points log: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        return pointsService.getByUserId(userId);
    }

    @Operation(summary = "增加积分", description = "为指定会员增加积分并记录来源、订单号与说明。")
    @PostMapping("/add")
    public boolean addPoints(@Parameter(description = "会员用户 ID") @PathVariable String userId, @RequestBody PointsRequest request) {
        log.info("[API] Add points: userId={}, points={}", (userId + "").replace("\r", "_").replace("\n", "_"), (request.getPoints() + "").replace("\r", "_").replace("\n", "_"));
        pointsService.addPoints(userId, request.getPoints(), request.getSource(), request.getOrderNo(), request.getDescription());
        return true;
    }

    @Operation(summary = "扣减积分", description = "扣减指定会员的积分并记录来源、订单号与说明。")
    @PostMapping("/deduct")
    public boolean deductPoints(@Parameter(description = "会员用户 ID") @PathVariable String userId, @RequestBody PointsRequest request) {
        log.info("[API] Deduct points: userId={}, points={}", (userId + "").replace("\r", "_").replace("\n", "_"), (request.getPoints() + "").replace("\r", "_").replace("\n", "_"));
        pointsService.deductPoints(userId, request.getPoints(), request.getSource(), request.getOrderNo(), request.getDescription());
        return true;
    }

    @Data
    public static class PointsRequest {
        private Integer points;
        private String source;
        private String orderNo;
        private String description;
    }
}
