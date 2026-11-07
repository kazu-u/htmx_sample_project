package com.example.htmx.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Spring MVC より前のフィルターで発生したセッション上限も503のエラーページへつなぐ。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionCapacityExceededFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            if (!hasSessionCapacityExceeded(exception) || response.isCommitted()) {
                throw exception;
            }
            response.resetBuffer();
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    private boolean hasSessionCapacityExceeded(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SessionCapacityExceededException) {
                return true;
            }
        }
        return false;
    }
}
