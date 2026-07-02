package com.scmcloud.common.rest.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.scmcloud.common.response.ApiResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Set;
import java.util.UUID;

/**
 * 鐢ㄦ埛鏈嶅姟瀹㈡埛绔紙@HttpExchange 鐗堟湰锟?
 * <p>鏇夸唬 OpenFeign 锟絊ysUserServiceClient</p>
 *
 * <p>鏋舵瀯璇存槑锟?
 * <ul>
 *   <li>涓昏閫氫俊锛欴ubbo (UserDubboService) - 楂樻€ц兘 RPC</li>
 *   <li>闄嶇骇澶囩敤锛歊estClient + @HttpExchange (SysUserServiceClient) - HTTP REST</li>
 * </ul>
 *
 * <p>姝ゅ鎴风锟絪ystem-service 锟絊ysUserController 绔偣瀵瑰簲
 *
 * <p>娉ㄦ剰锛氳璇佺浉鍏虫柟娉曪紙getUserByUsername, getUserRoles, getUserPermissions锟?
 * 搴斾娇锟紻ubbo 鑰屼笉锟紿TTP Exchange锛屽洜涓哄畠浠湪 controller 涓笉鍏紑
 *
 * @author Claude
 * @since 2025-12-29
 */
@NullMarked
@HttpExchange("/api/system/users")
public interface SysUserServiceClient {

    /**
     * 鏇存柊鏈€鍚庣櫥褰曚俊锟?
     * <p>瀵瑰簲: SysUserController.updateLastLogin()</p>
     * <p>Dubbo: UserDubboService.updateLastLogin()</p>
     *
     * <p>闄嶇骇绛栫暐锛氳繑鍥炴垚鍔燂紝涓嶄腑鏂櫥褰曟祦锟?p>
     *
     * @param userId 鐢ㄦ埛 ID
     * @param ipAddress IP 鍦板潃
     * @return 鍝嶅簲缁撴灉
     */
    @GetExchange("/{userId}/update-login")
    @SentinelResource(
        value = "user-service:updateLastLogin",
        fallback = "updateLastLoginFallback"
    )
    ApiResponse<Void> updateLastLogin(
        @PathVariable UUID userId,
        @RequestParam("ipAddress") String ipAddress
    );

    /**
     * 鏇存柊鏈€鍚庣櫥褰曚俊鎭殑闄嶇骇鏂规硶
     * <p>闄嶇骇绛栫暐锛氳繑鍥炴垚鍔燂紙涓嶅奖鍝嶇敤鎴风櫥褰曪級</p>
     * <p>Note: Sentinel Dashboard 浼氳褰曢檷绾т簨浠讹紝鏃犻渶搴旂敤鏃ュ織</p>
     *
     * @param userId 鐢ㄦ埛 ID
     * @param ipAddress IP 鍦板潃
     * @param ex 寮傚父
     * @return 鎴愬姛鍝嶅簲
     */
    default ApiResponse<Void> updateLastLoginFallback(
        UUID userId,
        String ipAddress,
        Throwable ex) {
        // 闄嶇骇杩斿洖鎴愬姛锛氱櫥褰曚俊鎭洿鏂板け璐ヤ笉搴斾腑鏂敤鎴风櫥褰曟祦锟?
        return ApiResponse.success();
    }

    @GetExchange("/{userId}/roles")
    ApiResponse<Set<String>> findRolesByUserId(@PathVariable UUID userId);

    @GetExchange("/{userId}/permissions")
    ApiResponse<Set<String>> findPermissionsByUserId(@PathVariable UUID userId);
}
