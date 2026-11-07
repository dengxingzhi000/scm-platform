package com.cognora.platform.common.dto.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * 批量用户操作请求。单次最多 100 个用户，避免同步请求长时间占用连接。
 */
@Data
public class BatchUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "用户 ID 列表不能为空")
    @Size(max = 100, message = "单次批量操作不能超过100个用户")
    private List<UUID> ids;

    /** 批量调岗时必填。 */
    private UUID deptId;

    /** 批量授权时必填。 */
    private List<UUID> roleIds;

    /** 批量禁用/删除原因，用于审计。 */
    private String reason;
}
