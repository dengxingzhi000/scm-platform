package com.scmcloud.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebAuthn 凭证响应DTO
 * <p>
 * 用于API响应，隐藏敏感信息（如公钥）
 * 参考Google Passkey API设计
 *
 * @author system
 * @since 2025-11-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebauthnCredentialDTO {
    private String credentialId;

    private String deviceName;

    private String algorithm;

    private UUID aaguid;

    private String transports;

    private String authenticatorAttachment;

    private Boolean isActive;

    private Boolean backupState;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}