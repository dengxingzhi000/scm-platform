package com.scmcloud.system.api;

import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.web.domain.SecurityUser;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Dubbo API 鐢ㄤ簬鏈嶅姟闂寸殑楂橀€熷唴锟絉PC 閫氫俊锟?
 * 鎻愪緵楂樻€ц兘鐨勭敤鎴风浉鍏虫搷浣滐紝鍖呮嫭韬唤楠岃瘉鍜屾巿鏉冿拷
 */
public interface UserDubboService {

    /**
     * 鏍规嵁鐢ㄦ埛鍚嶈幏鍙栫敤鎴疯繘琛岃韩浠介獙璇侊拷
     * 锟絊pring Security 锟経serDetailsService 浣跨敤锟?
     *
     * @param username 瑕佹悳绱㈢殑鐢ㄦ埛锟?
     * @return 鍖呭惈韬唤楠岃瘉淇℃伅锟絊ecurityUser 瀵硅薄锛屽鏋滄壘涓嶅埌鍒欒繑锟絥ull
     */
    SecurityUser getUserByUsername(String username);

    /**
     * Get user roles by userId.
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of role codes
     */
    Set<String> getUserRoles(UUID userId);

    /**
     * Get user permissions by userId.
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of permission codes
     */
    Set<String> getUserPermissions(UUID userId);

    /**
     * Fetch user info by userId.
     *
     * @param userId the user ID
     * @return UserInfo DTO
     */
    UserInfo getUserInfo(UUID userId);

    /**
     * Update last login info.
     *
     * @param userId the user ID
     * @param ipAddress the login IP address
     * @param loginTime the login timestamp
     */
    void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime);

    /**
     * Get username by userId.
     *
     * @param userId the user ID
     * @return UserDTO
     */
    UserDTO getUserById(UUID userId);

    /**
     * Get user roles by userId (alias for getUserRoles).
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of role codes
     */
    Set<String> findRolesByUserId(UUID userId);

    /**
     * Get user permissions by userId (alias for getUserPermissions).
     * Used for token refresh and authentication.
     *
     * @param userId the user ID
     * @return set of permission codes
     */
    Set<String> findPermissionsByUserId(UUID userId);
}

