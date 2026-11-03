package com.scmcloud.auth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WebAuthn 娉ㄥ唽璇锋眰DTO
 * <p>
 * 鐢ㄤ簬娉ㄥ唽鏂扮殑WebAuthn鍑瘉
 * 鍙傝€僃IDO2瑙勮寖鍜孏oogle Passkey瀹炵幇
 *
 * @author system
 * @since 2025-11-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebauthnRegistrationRequest {
    @NotBlank(message = "Credential ID cannot be empty")
    @Size(min = 16, max = 1024, message = "Credential ID length must be between 16-1024")
    private String credentialId;

    @NotBlank(message = "clientDataJSON cannot be empty")
    private String clientDataJSON;

    @NotBlank(message = "attestationObject cannot be empty")
    private String attestationObject;

    @NotBlank(message = "Device ID cannot be empty")
    private String deviceId;

    @Size(max = 2048, message = "Public key length cannot exceed 2048")
    private String publicKeyPem;

    @NotBlank(message = "Signature algorithm cannot be empty")
    @Pattern(regexp = "^(ES256|ES384|ES512|RS256|RS384|RS512|PS256|PS384|PS512|EdDSA)$",
             message = "Unsupported signature algorithm")
    private String algorithm;

    @Size(max = 100, message = "Device name length cannot exceed 100")
    private String deviceName;

    private UUID aaguid;

    @Size(max = 100, message = "Transport length cannot exceed 100")
    private String transports;

    @Pattern(regexp = "^(platform|cross-platform)$", message = "Invalid authenticator type")
    private String authenticatorAttachment;

    @Pattern(regexp = "^(required|preferred|discouraged)$", message = "Invalid user verification method")
    private String userVerification;

    private Boolean backupState;

    private Boolean backupEligible;
}