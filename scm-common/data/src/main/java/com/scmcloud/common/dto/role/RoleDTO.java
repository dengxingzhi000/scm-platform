package com.scmcloud.common.dto.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:43
 * @version 1.0
 */
@Data
public class RoleDTO {
    private UUID id;

    private UUID tenantId;

    @NotBlank(message = "Role code cannot be empty")
    @Pattern(regexp = "^ROLE_[A-Z_]+$", message = "Role code must start with ROLE_, only uppercase letters and underscores")
    private String roleCode;

    @NotBlank(message = "Role name cannot be empty")
    @Size(max = 64, message = "Role name length cannot exceed 64 characters")
    private String roleName;

    @Size(max = 255, message = "Role description length cannot exceed 255 characters")
    private String roleDesc;

    private String roleType;

    private String roleCategory;

    @NotNull(message = "Role level cannot be empty")
    @Min(value = 0, message = "Role level cannot be less than 0")
    @Max(value = 999, message = "Role level cannot exceed 999")
    private Integer roleLevel;

    @NotNull(message = "Data scope cannot be empty")
    private Integer dataScope;

    @DecimalMin(value = "0.00", message = "Max approval amount cannot be negative")
    private BigDecimal maxApprovalAmount;

    private String businessScope;

    private List<UUID> customDeptIds;

    private Integer status;

    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    // 鍏宠仈鏁版嵁
    private List<UUID> permissionIds;
    private List<UUID> deptIds;
    private Integer userCount; // 鎷ユ湁璇ヨ鑹茬殑鐢ㄦ埛锟?
}
