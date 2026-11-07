package com.cognora.platform.common.dto.user;

import lombok.Data;

/**
 *
 *
 * @author Deng
 * createData 2025/10/14 15:03
 * @version 1.0
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String twoFactorCode;
    private String deviceId;
    /**
     * 前端入口标识（{@code cognora-aura-web} / {@code cognora-aura-mall}）。
     * 为空时按管理台处理，兼容升级前的调用方；受众由服务端注册表决定。
     */
    private String clientId;
}