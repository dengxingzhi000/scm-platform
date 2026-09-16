package com.scmcloud.common.security.util;

import com.scmcloud.common.web.domain.SecurityUser;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 杩囨护鍣ㄦ梺璺鏌ュ伐鍏风被
 * <p>鎻愪緵鍏叡鐨勭櫧鍚嶅崟鍖归厤鍜屾梺璺垽鏂€昏緫锛屼緵澶氫釜瀹夊叏杩囨护鍣ㄥ锟?
 *
 * @author Deng
 * @version 1.0
 */
public final class FilterBypassHelper {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private FilterBypassHelper() {
    }

    /**
     * 妫€锟経RI鏄惁鍖归厤浠绘剰妯″紡
     *
     * @param uri      璇锋眰 URI
     * @param patterns 鍖归厤妯″紡鍒楄〃锛堟敮鎸丄nt椋庢牸锟?
     * @return 鏄惁鍖归厤
     */
    public static boolean matchesAny(String uri, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream()
                .anyMatch(p -> PATH_MATCHER.match(p, uri) || uri.startsWith(p));
    }

    /**
     * 妫€鏌ユ槸鍚﹀簲璇ユ梺璺繃婊ゅ櫒
     *
     * @param uri               璇锋眰 URI
     * @param user              褰撳墠鐢ㄦ埛
     * @param bypassPaths       鏃佽矾璺緞鍒楄〃
     * @param bypassUsers       鏃佽矾鐢ㄦ埛鍒楄〃
     * @param bypassRoles       鏃佽矾瑙掕壊鍒楄〃
     * @param bypassPermissions 鏃佽矾鏉冮檺鍒楄〃
     * @return 鏄惁搴旇鏃佽矾
     */
    public static boolean shouldBypass(String uri, SecurityUser user, List<String> bypassPaths, List<String> bypassUsers,
                                       List<String> bypassRoles, List<String> bypassPermissions) {
        // 1. 妫€鏌ヨ矾寰勫尮锟?
        if (matchesAny(uri, bypassPaths)) {
            return true;
        }

        if (user == null) {
            return false;
        }

        // 2. 妫€鏌ョ敤鎴峰悕鍖归厤
        if (bypassUsers != null && bypassUsers.stream()
                .anyMatch(u -> Objects.equals(u, user.getUsername()))) {
            return true;
        }

        // 3. 妫€鏌ヨ鑹插尮锟?
        Set<String> roles = user.getRoles();
        if (roles != null && bypassRoles != null &&
                roles.stream().anyMatch(bypassRoles::contains)) {
            return true;
        }

        // 4. 妫€鏌ユ潈闄愬尮锟?
        Set<String> permissions = user.getPermissions();
        return permissions != null && bypassPermissions != null &&
                permissions.stream()
                        .anyMatch(bypassPermissions::contains);
    }
}