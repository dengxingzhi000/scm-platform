package com.scmcloud.common.security.handler;

import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.security.util.SecurityErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 璁よ瘉鍏ュ彛锟?
 *
 * @author Deng
 * createData 2025/10/14 14:58
 * @version 1.0
 */
@Component
@Slf4j
@NullMarked
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Spring Security puts the rejected principal and the reason into this message, so it stays in the
        // log and the client only ever receives a stable code.
        log.warn("Unauthorized access attempt: {} from IP: {}, reason: {}",
                request.getRequestURI(), IpUtils.getClientIp(request), authException.getMessage());

        SecurityErrorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHENTICATED", "未授权访问，请先登录");
    }
}
