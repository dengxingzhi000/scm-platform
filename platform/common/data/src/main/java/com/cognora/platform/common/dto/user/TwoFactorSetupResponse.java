package com.cognora.platform.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 2FA 初始化响应。秘钥只在此处返回一次，前端据此渲染 otpauth 二维码。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String secret;
    private String otpauthUri;
}
