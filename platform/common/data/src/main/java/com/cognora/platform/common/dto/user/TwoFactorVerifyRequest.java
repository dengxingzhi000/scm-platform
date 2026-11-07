package com.cognora.platform.common.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 2FA 校验请求：TOTP 6 位验证码或一次性恢复码。
 */
@Data
public class TwoFactorVerifyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "验证码不能为空")
    @Size(max = 32, message = "验证码格式不正确")
    private String code;
}
