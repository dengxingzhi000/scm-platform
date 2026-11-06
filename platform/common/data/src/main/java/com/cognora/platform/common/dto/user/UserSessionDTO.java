package com.cognora.platform.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 当前用户的登录会话视图，对应前端 {@code AuthSessionDTO}。
 *
 * <p>只暴露设备标识、IP 与时间信息，不返回访问令牌本身。后端尚未采集设备名称、
 * 设备类型、地理位置与 User-Agent，因此这些字段保持 {@code null}，由前端展示为
 * 「未知设备」，不用伪造值填充。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 会话标识：令牌的 deviceId 声明。 */
    private String sessionId;

    /** 签发令牌时的客户端 IP。 */
    private String ip;

    /** 最近活跃时间：取令牌最近一次签发/刷新时间。 */
    private Instant lastActiveAt;

    /** 是否为当前请求令牌所对应的会话。 */
    private boolean current;
}
