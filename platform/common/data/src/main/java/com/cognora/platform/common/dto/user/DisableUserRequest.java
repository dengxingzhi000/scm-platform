package com.cognora.platform.common.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 禁用用户请求。
 */
@Data
public class DisableUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 255, message = "禁用原因不能超过255个字符")
    private String reason;
}
