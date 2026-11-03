package com.scmcloud.common.dto.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 临时角色授予 DTO
 *
 * @author Deng
 * createData 2025/11/3 15:57
 * @version 1.0
 */
@Data
public class TemporaryRoleGrantDTO {
    @NotNull(message = "角色列表不能为空")
    private List<UUID> roleIds;

    @NotNull(message = "生效时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime effectiveTime;

    @NotNull(message = "过期时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    @NotBlank(message = "授予原因不能为空")
    @Size(max = 500, message = "授予原因不能超过500字符")
    private String reason;
}
