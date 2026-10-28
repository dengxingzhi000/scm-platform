package com.scmcloud.common.security.handler;

import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.security.util.SecurityErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Jwt 鎷掔粷澶勭悊锟?
 *
 * @author Deng
 * createData 2025/10/11 13:55
 * @version 1.0
 */
@Component
@Slf4j
@NullMarked
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // Spring Security quotes the principal and the missing authority in this message.
        log.warn("Access denied: {} for user: {} from IP: {}, reason: {}",
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
                IpUtils.getClientIp(request), accessDeniedException.getMessage());

        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", "Insufficient permissions, access denied");
    }
}
