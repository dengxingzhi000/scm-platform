package com.scmcloud.common.dto.user;

import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.web.annotation.Sensitive;
import com.scmcloud.common.web.enums.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/15 11:46
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private UUID userId;
    private String username;
    @Sensitive(type = SensitiveType.NAME)
    private String realName;
    private String avatar;
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;
    @Sensitive(type = SensitiveType.MOBILE)
    private String phone;
    private UUID deptId;
    private String deptName;
    private Integer userLevel;
    private Set<String> roles;
    private Set<String> permissions;
    private Set<PermissionDTO> menuTree; // 鑿滃崟锟?
}
