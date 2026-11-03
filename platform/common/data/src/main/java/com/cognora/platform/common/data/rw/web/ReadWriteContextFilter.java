package com.cognora.platform.common.data.rw.web;

import com.cognora.platform.common.data.rw.routing.ReadWriteRoutingContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Marks the current thread as a servlet request thread for read-write routing.
 * <p>
 * Two effects:
 * <ul>
 *   <li>the SQL layer is allowed to send unannotated SELECTs to a slave during a request;</li>
 *   <li>routing state (slave selection, read-after-write marker) is cleared at the end
 *       of every request, so pooled Tomcat threads never leak it into the next request.</li>
 * </ul>
 *
 * @author Deng
 * @since 2025-12-16
 */
public class ReadWriteContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ReadWriteRoutingContext.beginRequestScope();
        try {
            filterChain.doFilter(request, response);
        } finally {
            ReadWriteRoutingContext.endRequestScope();
        }
    }
}