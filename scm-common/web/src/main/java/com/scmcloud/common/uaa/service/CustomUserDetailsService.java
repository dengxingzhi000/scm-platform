package com.scmcloud.common.uaa.service;

import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.api.UserDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService Implementation
 * Uses Dubbo RPC for high-performance user authentication
 *
 * @author Deng
 * @version 2.0
 * createData 2025/10/24 14:36
 */
@Service
@RequiredArgsConstructor
@Slf4j
@NullMarked
public class CustomUserDetailsService implements UserDetailsService {
    private final UserDubboService userDubboService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 閫氳繃 Dubbo 楂樻€ц兘 RPC 鑾峰彇鐢ㄦ埛淇℃伅锛堝寘鍚瘑鐮併€佽鑹层€佹潈闄愶級
        SecurityUser user = userDubboService.getUserByUsername(username);

        if (user == null) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException("鐢ㄦ埛涓嶅瓨锟?" + username);
        }

        log.debug("User loaded: {}, roles: {}, permissions: {}",
                username, user.getRoles().size(), user.getPermissions().size());

        return user;
    }
}
