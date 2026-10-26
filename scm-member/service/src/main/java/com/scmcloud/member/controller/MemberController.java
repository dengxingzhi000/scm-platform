package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.service.IMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "会员管理", description = "会员注册、信息查询、资料更新与等级调整管理")
public class MemberController {
    private final IMemberService memberService;

    @Operation(summary = "会员注册", description = "根据用户 ID、昵称与头像为用户注册会员账号。")
    @PostMapping("/register")
    public Member register(@RequestBody RegisterRequest request) {
        log.info("[API] Register member: userId={}", request.getUserId());
        return memberService.register(request.getUserId(), request.getNickname(), request.getAvatar());
    }

    @Operation(summary = "查询会员信息", description = "按用户 ID 查询会员档案信息。")
    @GetMapping("/{userId}")
    public Member getByUserId(@Parameter(description = "会员用户 ID") @PathVariable String userId) {
        log.info("[API] Get member: userId={}", userId);
        return memberService.getByUserId(userId);
    }

    @Operation(summary = "更新会员信息", description = "按用户 ID 更新会员昵称、头像等基础资料。")
    @PutMapping("/{userId}")
    public boolean update(@Parameter(description = "会员用户 ID") @PathVariable String userId, @RequestBody Member member) {
        log.info("[API] Update member: userId={}", userId);
        member.setUserId(userId);
        return memberService.updateById(member);
    }

    @Operation(summary = "更新会员等级", description = "调整指定会员的会员等级编号。")
    @PutMapping("/{userId}/level")
    public boolean updateLevel(@Parameter(description = "会员用户 ID") @PathVariable String userId, @Parameter(description = "目标会员等级编号") @RequestParam Integer level) {
        log.info("[API] Update member level: userId={}, level={}", userId, level);
        memberService.updateMemberLevel(userId, level);
        return true;
    }

    @Data
    public static class RegisterRequest {
        private String userId;
        private String nickname;
        private String avatar;
    }
}
