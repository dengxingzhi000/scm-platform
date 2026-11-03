package com.cognora.ecom.member.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.ecom.member.domain.entity.Member;
import com.cognora.ecom.member.service.IMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "会员管理", description = "会员注册、分页查询、资料更新与等级调整管理")
public class MemberController {
    private final IMemberService memberService;

    @Operation(summary = "分页查询会员", description = "按昵称/会员号/用户 ID 关键字、等级与状态分页查询会员。")
    @GetMapping("/page")
    public Page<Member> page(
            @Parameter(description = "页码，默认 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "昵称、会员号或用户 ID") @RequestParam(required = false) String keyword,
            @Parameter(description = "会员等级") @RequestParam(required = false) Integer memberLevel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return memberService.pageQuery(page, size, keyword, memberLevel, status);
    }

    @Operation(summary = "会员注册", description = "根据用户 ID、昵称与头像为用户注册会员账号；重复注册返回已有会员。")
    @PostMapping("/register")
    public Member register(@Valid @RequestBody RegisterRequest request) {
        log.info("[API] Register member: userId={}", (request.getUserId() + "").replace("\r", "_").replace("\n", "_"));
        return memberService.register(request.getUserId(), request.getNickname(), request.getAvatar());
    }

    @Operation(summary = "查询会员信息", description = "按用户 ID 查询会员档案信息。")
    @GetMapping("/{userId}")
    public Member getByUserId(@Parameter(description = "会员用户 ID") @PathVariable String userId) {
        log.info("[API] Get member: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        return memberService.getByUserId(userId);
    }

    @Operation(summary = "更新会员信息", description = "按用户 ID 更新昵称、头像、性别与生日；会员号、积分、消费额由系统维护。")
    @PutMapping("/{userId}")
    public Member update(@Parameter(description = "会员用户 ID") @PathVariable String userId,
                         @Valid @RequestBody UpdateProfileRequest request) {
        log.info("[API] Update member: userId={}", (userId + "").replace("\r", "_").replace("\n", "_"));
        return memberService.updateProfile(userId, request.getNickname(), request.getAvatar(),
                request.getGender(), request.getBirthday());
    }

    @Operation(summary = "更新会员等级", description = "调整指定会员的会员等级编号。")
    @PutMapping("/{userId}/level")
    public boolean updateLevel(@Parameter(description = "会员用户 ID") @PathVariable String userId,
                               @Parameter(description = "目标会员等级编号") @RequestParam Integer level) {
        log.info("[API] Update member level: userId={}, level={}", (userId + "").replace("\r", "_").replace("\n", "_"), (level + "").replace("\r", "_").replace("\n", "_"));
        memberService.updateMemberLevel(userId, level);
        return true;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户 ID 不能为空")
        private String userId;

        @Size(max = 64, message = "昵称长度不能超过 64")
        private String nickname;

        @Size(max = 512, message = "头像地址长度不能超过 512")
        private String avatar;
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 64, message = "昵称长度不能超过 64")
        private String nickname;

        @Size(max = 512, message = "头像地址长度不能超过 512")
        private String avatar;

        @Parameter(description = "性别：0-未知，1-男，2-女")
        private Integer gender;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate birthday;
    }
}
