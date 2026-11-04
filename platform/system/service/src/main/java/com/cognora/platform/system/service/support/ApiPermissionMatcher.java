package com.cognora.platform.system.service.support;

import com.cognora.platform.system.domain.entity.SysPermission;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将请求 URL 解析为需要的权限码。
 *
 * <p>匹配规则：</p>
 * <ol>
 *   <li>字面量 {@code api_path} 先按字符串全等匹配，保持匹配器升级前的行为；</li>
 *   <li>含 {@code {var}} 占位符或 Ant 通配符（{@code *}、{@code **}、{@code ?}）的模板走
 *       {@link AntPathMatcher}；</li>
 *   <li>{@code http_method} 语义不变：等于请求方法或为 {@code *}，其余（含 NULL）不命中。</li>
 * </ol>
 *
 * <p>匹配器只负责“命中的权限码集合”，不做默认拒绝：没有命中时返回空列表。</p>
 */
public final class ApiPermissionMatcher {

    private static final AntPathMatcher ANT_MATCHER = new AntPathMatcher();

    private ApiPermissionMatcher() {
    }

    /**
     * @param apiPath    权限登记里的 api_path（字面量或模板）
     * @param requestPath 实际请求路径
     * @return 是否命中；任一参数为 null 时返回 false
     */
    public static boolean matches(String apiPath, String requestPath) {
        if (apiPath == null || requestPath == null) {
            return false;
        }
        if (apiPath.equals(requestPath)) {
            return true;
        }
        if (!isPattern(apiPath)) {
            return false;
        }
        return ANT_MATCHER.match(apiPath, requestPath);
    }

    /**
     * 从候选权限行中解析出命中的权限码，保持首次出现的顺序并去重。
     *
     * @param candidates 数据库按 {@code http_method} 预筛后的候选行
     * @param requestPath 实际请求路径
     * @param method      实际请求方法
     */
    public static List<String> resolve(List<SysPermission> candidates, String requestPath, String method) {
        Set<String> codes = new LinkedHashSet<>();
        if (candidates == null || requestPath == null) {
            return new ArrayList<>(codes);
        }
        for (SysPermission candidate : candidates) {
            if (candidate == null || candidate.getPermissionCode() == null) {
                continue;
            }
            String httpMethod = candidate.getHttpMethod();
            if (httpMethod == null) {
                continue;
            }
            if (!"*".equals(httpMethod) && !httpMethod.equalsIgnoreCase(method)) {
                continue;
            }
            if (matches(candidate.getApiPath(), requestPath)) {
                codes.add(candidate.getPermissionCode());
            }
        }
        return new ArrayList<>(codes);
    }

    private static boolean isPattern(String apiPath) {
        return apiPath.indexOf('{') >= 0
                || apiPath.indexOf('*') >= 0
                || apiPath.indexOf('?') >= 0;
    }
}
