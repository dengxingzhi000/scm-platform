package com.cognora.platform.auth.service;

import com.cognora.platform.common.dto.user.LoginRequest;
import com.cognora.platform.common.dto.user.LoginResponse;
import com.cognora.platform.common.dto.user.UserInfo;
import com.cognora.platform.common.dto.user.UserSessionDTO;

import java.util.List;
import java.util.UUID;

/**
 * 绯荤粺璁よ瘉鏈嶅姟鎺ュ彛
 * 
 * <p>鎻愪緵鏍稿績鐨勮璇佸姛鑳斤紝鍖呮嫭鐢ㄦ埛鐧诲綍銆佺櫥鍑恒€佷护鐗屽埛鏂扮瓑鎿嶄綔</p>
 * 
 * @author system
 * @since 2025-11-27
 */
public interface ISysAuthService {

    /**
     * 鐢ㄦ埛鐧诲綍
     * 
     * @param request 鐧诲綍璇锋眰鍙傛暟锛屽寘鍚敤鎴峰悕鍜屽瘑鐮佺瓑淇℃伅
     * @param ipAddress 瀹㈡埛锟絀P鍦板潃
     * @param deviceId 璁惧 ID
     * @return 鐧诲綍鍝嶅簲锛屽寘鍚闂护鐗屽拰鍒锋柊浠ょ墝
     */
    LoginResponse login(LoginRequest request, String ipAddress, String deviceId);

    /**
     * 鐢ㄦ埛鐧诲嚭
     * 
     * @param token 璁块棶浠ょ墝
     * @param userId 鐢ㄦ埛 ID
     * @param reason 鐧诲嚭鍘熷洜
     */
    void logout(String token, UUID userId, String reason);

    /**
     * 鍒锋柊璁块棶浠ょ墝
     * 
     * @param refreshToken 鍒锋柊浠ょ墝
     * @param deviceId 璁惧 ID
     * @param ipAddress 瀹㈡埛锟絀P鍦板潃
     * @return 鏂扮殑鐧诲綍鍝嶅簲锛屽寘鍚柊鐨勮闂护鐗屽拰鍒锋柊浠ょ墝
     */
    LoginResponse refreshToken(String refreshToken, String deviceId, String ipAddress);

    /**
     * 寮哄埗鐢ㄦ埛鐧诲嚭
     * 
     * @param userId 鐢ㄦ埛 ID
     * @param reason 寮哄埗鐢ㄦ埛鐧诲嚭鐨勫師锟?
     */
    void forceLogout(UUID userId, String reason);

    /**
     * 鑾峰彇鑾峰彇鐢ㄦ埛淇℃伅
     * 
     * @param userId 鐢ㄦ埛 ID
     * @return 鐢ㄦ埛淇℃伅
     */
    UserInfo getUserInfo(UUID userId);

    /**
     * 列出当前访问令牌所属用户的全部有效会话。
     *
     * <p>用户身份只从传入的访问令牌解析，不接受调用方指定 userId。</p>
     *
     * @param accessToken 当前请求携带的访问令牌
     * @return 会话列表，当前令牌对应的会话标记为 current
     */
    List<UserSessionDTO> listSessions(String accessToken);

    /**
     * 撤销当前用户除当前设备外的全部访问令牌。
     *
     * <p>用户身份与保留设备均来自传入的访问令牌，不接受 userId 入参。</p>
     *
     * @param accessToken 当前请求携带的访问令牌
     * @return 实际撤销的会话数量
     */
    int logoutOtherSessions(String accessToken);
}
