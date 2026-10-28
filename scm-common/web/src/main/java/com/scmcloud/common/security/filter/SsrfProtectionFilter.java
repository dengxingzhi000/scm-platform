package com.scmcloud.common.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SsrfProtectionFilter implements Filter {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "169.254.169.254",
        "metadata.google.internal",
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1"
    );

    private static final Set<String> BLOCKED_PREFIXES = Set.of(
        "10.",
        "172.16.", "172.17.", "172.18.", "172.19.",
        "172.20.", "172.21.", "172.22.", "172.23.",
        "172.24.", "172.25.", "172.26.", "172.27.",
        "172.28.", "172.29.", "172.30.", "172.31.",
        "192.168."
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String urlParam = httpRequest.getParameter("url");
        if (urlParam != null && !urlParam.isEmpty()) {
            if (isBlockedUrl(urlParam)) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.getWriter().write("{\"code\":400,\"message\":\"Invalid URL\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isBlockedUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                // A leading-slash value is a path, not a fetch target: ApiAccessControlFilter looks
                // permissions up with url=/api/v1/orders/query, and blocking that fails every
                // business API closed. Anything else without an authority stays blocked.
                return !url.startsWith("/");
            }

            if (BLOCKED_HOSTS.contains(host.toLowerCase())) {
                return true;
            }

            for (String prefix : BLOCKED_PREFIXES) {
                if (host.startsWith(prefix)) {
                    return true;
                }
            }

            try {
                InetAddress address = InetAddress.getByName(host);
                String resolvedHost = address.getHostAddress();
                if (BLOCKED_HOSTS.contains(resolvedHost)) {
                    return true;
                }
                for (String prefix : BLOCKED_PREFIXES) {
                    if (resolvedHost.startsWith(prefix)) {
                        return true;
                    }
                }
            } catch (UnknownHostException e) {
                // Host cannot be resolved, allow through
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
