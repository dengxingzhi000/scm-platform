package com.scmcloud.auth.domain.dto;

import com.scmcloud.auth.domain.entity.WebauthnCredential;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WebAuthn 鍑瘉杞崲锟?
 * <p>
 * 瀹炵幇Entity鍜孌TO涔嬮棿鐨勮浆锟?
 * 闅愯棌鏁忔劅淇℃伅锛屼繚鎶ゆ暟鎹畨锟?
 *
 * @author system
 * @since 2025-11-27
 */
@Component
public class WebauthnCredentialConverter {
    /**
     * 灏嗗疄浣撹浆鎹负DTO
     * 绉婚櫎鏁忔劅淇℃伅锛堝叕閽ョ瓑锟?
     *
     * @param entity 瀹炰綋瀵硅薄
     * @return DTO 瀵硅薄
     */
    public WebauthnCredentialDTO toDTO(WebauthnCredential entity) {
        if (entity == null) {
            return null;
        }

        return WebauthnCredentialDTO.builder()
                .credentialId(entity.getCredentialId())
                .deviceName(entity.getDeviceName())
                .algorithm(entity.getAlg())
                .aaguid(entity.getAaguid())
                .transports(entity.getTransports() != null ? String.join(",", entity.getTransports()) : null)
                .authenticatorAttachment(null) // Not stored in database
                .isActive(entity.getIsActive())
                .backupState(null) // Not stored in database
                .lastUsedAt(entity.getLastUsedAt())
                .createdTime(entity.getCreateTime())
                .build();
    }

    /**
     * 鎵归噺杞崲
     *
     * @param entities 瀹炰綋鍒楄〃
     * @return DTO 鍒楄〃
     */
    public List<WebauthnCredentialDTO> toDTOList(List<WebauthnCredential> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 灏嗘敞鍐岃姹傝浆鎹负瀹炰綋
     *
     * @param request 娉ㄥ唽璇锋眰
     * @param userId  鐢ㄦ埛 ID
     * @return 瀹炰綋瀵硅薄
     */
    public WebauthnCredential toEntity(WebauthnRegistrationRequest request, UUID userId) {
        if (request == null) {
            return null;
        }

        WebauthnCredential entity = new WebauthnCredential();
        entity.setCredentialId(request.getCredentialId());
        entity.setUserId(userId);
        entity.setPublicKeyPem(request.getPublicKeyPem());
        entity.setAlg(request.getAlgorithm());
        entity.setSignCount(0L); // 鍒濆璁℃暟锟?
        entity.setDeviceName(request.getDeviceName());
        entity.setAaguid(request.getAaguid());
        entity.setTransports(request.getTransports() != null ? request.getTransports().split(",") : null);
        entity.setIsActive(true); // 榛樿鍚敤

        return entity;
    }
}