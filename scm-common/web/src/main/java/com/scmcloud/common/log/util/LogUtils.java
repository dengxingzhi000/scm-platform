package com.scmcloud.common.log.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson2.JSON;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * 鏃ュ織宸ュ叿锟?
 *
 * @author Deng
 * createData 2025/10/24 14:03
 * @version 1.0
 */
@Slf4j
public class LogUtils {
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_TRACE_ID = "traceId";
    private static final String KEY_REQUEST_ID = "requestId";

    /**
     * 璁剧疆鐢ㄦ埛涓婁笅锟?
     */
    public static void setUserContext(Long userId, String username) {
        if (userId != null) {
            MDC.put(KEY_USER_ID, userId.toString());
        }
        if (StrUtil.isNotBlank(username)) {
            MDC.put(KEY_USERNAME, username);
        }
    }

    /**
     * 璁剧疆杩借釜 ID
     */
    public static void setTraceId(String traceId) {
        if (StrUtil.isNotBlank(traceId)) {
            MDC.put(KEY_TRACE_ID, traceId);
        }
    }

    /**
     * 璁剧疆璇锋眰 ID
     */
    public static void setRequestId(String requestId) {
        if (StrUtil.isNotBlank(requestId)) {
            MDC.put(KEY_REQUEST_ID, requestId);
        }
    }

    /**
     * 娓呴櫎 MDC
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 缁撴瀯鍖栨棩锟? 涓氬姟鏃ュ織
     */
    public static void business(String action, String result, Object data) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("type", "business");
        logMap.put("action", action);
        logMap.put("result", result);
        logMap.put("data", data);
        logMap.put("timestamp", System.currentTimeMillis());

        log.info("BUSINESS_LOG: {}", JSON.toJSONString(logMap));
    }

    /**
     * 缁撴瀯鍖栨棩锟? 鎺ュ彛璋冪敤
     */
    public static void api(String method, String uri, long duration, int status) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("type", "api");
        logMap.put("method", method);
        logMap.put("uri", uri);
        logMap.put("duration", duration);
        logMap.put("status", status);
        logMap.put("timestamp", System.currentTimeMillis());

        log.info("API_LOG: {}", JSON.toJSONString(logMap));
    }

    /**
     * 缁撴瀯鍖栨棩锟? RPC璋冪敤
     */
    public static void rpc(String service, String method, long duration, boolean success, String error) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("type", "rpc");
        logMap.put("service", service);
        logMap.put("method", method);
        logMap.put("duration", duration);
        logMap.put("success", success);
        logMap.put("error", error);
        logMap.put("timestamp", System.currentTimeMillis());

        log.info("RPC_LOG: {}", JSON.toJSONString(logMap));
    }
}