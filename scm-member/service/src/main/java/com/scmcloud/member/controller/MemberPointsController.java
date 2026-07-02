package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.MemberPointsLog;
import com.scmcloud.member.service.IMemberPointsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members/{userId}/points")
public class MemberPointsController {
    private final IMemberPointsService pointsService;

    @GetMapping
    public int getBalance(@PathVariable String userId) {
        log.info("[API] Get points balance: userId={}", userId);
        return pointsService.getPointsBalance(userId);
    }

    @GetMapping("/log")
    public List<MemberPointsLog> getLog(@PathVariable String userId) {
        log.info("[API] Get points log: userId={}", userId);
        return pointsService.getByUserId(userId);
    }

    @PostMapping("/add")
    public boolean addPoints(@PathVariable String userId, @RequestBody PointsRequest request) {
        log.info("[API] Add points: userId={}, points={}", userId, request.getPoints());
        pointsService.addPoints(userId, request.getPoints(), request.getSource(), request.getOrderNo(), request.getDescription());
        return true;
    }

    @PostMapping("/deduct")
    public boolean deductPoints(@PathVariable String userId, @RequestBody PointsRequest request) {
        log.info("[API] Deduct points: userId={}, points={}", userId, request.getPoints());
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
