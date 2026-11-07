package com.cognora.platform.common.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 重置密码结果。
 *
 * <p>{@code temporaryPassword} 仅在调用方持有 {@code system:user:reset:reveal}
 * 权限时返回；否则为 null，避免明文密码在默认响应中扩散。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String temporaryPassword;

    private boolean mustChangePassword;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
}
