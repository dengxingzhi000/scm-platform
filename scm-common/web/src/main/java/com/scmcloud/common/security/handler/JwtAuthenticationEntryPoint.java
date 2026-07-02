package com.scmcloud.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.security.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt: {} from IP: {}",
                request.getRequestURI(), IpUtils.getClientIp(request));

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", "鏈巿鏉冭闂紝璇峰厛鐧诲綍");
        result.put("error", authException.getMessage());
        result.put("path", request.getRequestURI());
        result.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}