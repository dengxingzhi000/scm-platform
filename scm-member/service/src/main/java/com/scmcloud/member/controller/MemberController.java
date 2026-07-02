package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.service.IMemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {
    private final IMemberService memberService;

    @PostMapping("/register")
    public Member register(@RequestBody RegisterRequest request) {
        log.info("[API] Register member: userId={}", request.getUserId());
        return memberService.register(request.getUserId(), request.getNickname(), request.getAvatar());
    }

    @GetMapping("/{userId}")
    public Member getByUserId(@PathVariable String userId) {
        log.info("[API] Get member: userId={}", userId);
        return memberService.getByUserId(userId);
    }

    @PutMapping("/{userId}")
    public boolean update(@PathVariable String userId, @RequestBody Member member) {
        log.info("[API] Update member: userId={}", userId);
        member.setUserId(userId);
        return memberService.updateById(member);
    }

    @PutMapping("/{userId}/level")
    public boolean updateLevel(@PathVariable String userId, @RequestParam Integer level) {
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
