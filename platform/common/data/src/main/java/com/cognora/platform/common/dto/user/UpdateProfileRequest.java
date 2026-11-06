package com.cognora.platform.common.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户可自助修改的资料字段白名单。
 *
 * <p>只接受昵称、头像、手机、邮箱、时区五项；用户名、密码、状态、部门、租户等字段
 * 一律不在此 DTO 中，避免被批量赋值。字段为 {@code null} 表示不修改。</p>
 *
 * <p>昵称落库到 {@code sys_user.real_name}（该表沿用「真实姓名」作为展示名）。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
public class UpdateProfileRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(min = 1, max = 64, message = "昵称长度1-64个字符")
    private String realName;

    @Size(max = 512, message = "头像地址不能超过512个字符")
    private String avatar;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    @Pattern(
            regexp = "^[A-Za-z][A-Za-z0-9_+/-]{0,63}$",
            message = "时区格式不正确，例如 Asia/Shanghai"
    )
    private String timezone;
}
