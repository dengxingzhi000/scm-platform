package com.scmcloud.common.log.interceptor;

import com.scmcloud.common.log.util.LogUtils;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.web.util.SecurityUtils;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 *
 *
 * @author Deng
 * createData 2025/10/24 14:05
 * @version 1.0
 */
@Slf4j
@NullMarked
public class LogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
                             @Nonnull Object handler) {
        // 璁剧疆 RequestId
        String requestId = UUIDv7Util.generateString().replace("-", "");
        LogUtils.setRequestId(requestId);
        response.setHeader("X-Request-Id", requestId);

        // 璁剧疆鐢ㄦ埛涓婁笅锟?
        Long userId = null;
        String username = SecurityUtils.getCurrentUsername().orElse(null);
        if (SecurityUtils.getCurrentUserUuid().isPresent()) {
            userId = SecurityUtils.getCurrentUserUuid()
                    .map(UUID::getMostSignificantBits)
                    .orElse(null);
        }
        LogUtils.setUserContext(userId, username);

        // 璁板綍璇锋眰寮€锟?
        request.setAttribute("startTime", System.currentTimeMillis());

        return true;
    }

    @Override
    public void afterCompletion(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
                                @Nonnull Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                LogUtils.api(request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            }
        } finally {
            LogUtils.clear();
        }
    }
}
