package com.frog.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WebAuthn 注册请求DTO
 * <p>
 * 用于注册新的WebAuthn凭证
 * 参考FIDO2规范和Google Passkey实现
 *
 * @author system
 * @since 2025-11-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebauthnRegistrationRequest {
    @NotBlank(message = "凭证 ID不能为空")
    @Size(min = 16, max = 1024, message = "凭证ID长度必须在16-1024之间")
    private String credentialId;

    @NotBlank(message = "clientDataJSON 不能为空")
    private String clientDataJSON;

    @NotBlank(message = "attestationObject 不能为空")
    private String attestationObject;

    @NotBlank(message = "设备 ID不能为空")
    private String deviceId;

    @Size(max = 2048, message = "公钥长度不能超过2048")
    private String publicKeyPem;

    @NotBlank(message = "签名算法不能为空")
    @Pattern(regexp = "^(ES256|ES384|ES512|RS256|RS384|RS512|PS256|PS384|PS512|EdDSA)$",
             message = "不支持的签名算法")
    private String algorithm;

    @Size(max = 100, message = "设备名称长度不能超过100")
    private String deviceName;

    private UUID aaguid;

    @Size(max = 100, message = "传输方式长度不能超过100")
    private String transports;

    @Pattern(regexp = "^(platform|cross-platform)$", message = "认证器类型不正确")
    private String authenticatorAttachment;

    @Pattern(regexp = "^(required|preferred|discouraged)$", message = "用户验证方法不正确")
    private String userVerification;

    private Boolean backupState;

    private Boolean backupEligible;
}