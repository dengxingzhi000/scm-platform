package com.scmcloud.common.dto.permission;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:45
 * @version 1.0
 */
@Data
public class PermissionDTO {
    private UUID id;

    @NotNull(message = "Parent ID cannot be empty")
    private UUID parentId;

    @NotBlank(message = "Permission code cannot be empty")
    @Pattern(regexp = "^[a-z][a-z0-9:]*$", message = "Permission code can only contain lowercase letters, numbers and colons")
    private String permissionCode;

    @NotBlank(message = "Permission name cannot be empty")
    @Size(max = 64, message = "Permission name length cannot exceed 64 characters")
    private String permissionName;

    @NotNull(message = "Permission type cannot be empty")
    @Min(value = 1, message = "Permission type must be between 1-5")
    @Max(value = 5, message = "Permission type must be between 1-5")
    private Integer permissionType;

    private String routePath;

    private String component;

    private String redirect;

    private String icon;

    private String apiPath;

    private String httpMethod;

    @NotBlank(message = "Permission scope cannot be empty")
    @Pattern(regexp = "^(PLATFORM|TENANT)$", message = "Permission scope must be PLATFORM or TENANT")
    private String permissionScope;

    @NotNull(message = "Permission level cannot be empty")
    @Min(value = 1, message = "Permission level must be between 1-4")
    @Max(value = 4, message = "Permission level must be between 1-4")
    private Integer permissionLevel;

    @NotNull(message = "Risk level cannot be empty")
    @Min(value = 1, message = "Risk level must be between 1-4")
    @Max(value = 4, message = "Risk level must be between 1-4")
    private Integer riskLevel;

    private Boolean needApproval;

    private Boolean needTwoFactor;

    private Integer sortOrder;

    private Boolean visible;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    // 鏍戝舰缁撴瀯
    private List<PermissionDTO> children;
}
