package com.cognora.platform.system.service.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.dto.user.UserDTO;
import com.cognora.platform.common.dto.user.UserInfo;
import com.cognora.platform.common.web.domain.SecurityUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Query-side service for {@link com.cognora.platform.system.domain.entity.SysUser}.
 *
 * <p>All methods are read-only. Implementations must be annotated with
 * {@link com.cognora.platform.common.data.rw.annotation.Slave}.</p>
 */
public interface ISysUserQueryService {

    Page<UserDTO> listUsers(Integer pageNum, Integer pageSize, String username, Integer status);

    SecurityUser getUserByUsername(String username);

    UserDTO getUserById(UUID id);

    UserInfo getUserInfo(UUID userId);

    List<Map<String, Object>> getUserTemporaryRoles(UUID userId);

    boolean canAccessDept(UUID userId, UUID deptId);

    Integer getUserDataScope(UUID userId);

    Map<String, Object> getUserStatistics(UUID userId);
}
