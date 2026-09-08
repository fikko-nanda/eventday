package com.example.eventday.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class ApiLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String reqId = UUID.randomUUID().toString().substring(0, 8);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        String fullUri = query != null ? uri + "?" + query : uri;

        // Wrap response untuk bisa baca status setelah chain
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            log.info("[API HIT] [{}] {} {} from={} UA={}", reqId, method, fullUri, ip, userAgent != null ? userAgent.substring(0, Math.min(60, userAgent.length())) : "-");
            chain.doFilter(request, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = wrappedResponse.getStatus();
            String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "-";
            // Ambil JWT userId/role dari header jika ada (debug)
            String authHeader = request.getHeader("Authorization");
            String hasToken = (authHeader != null && authHeader.startsWith("Bearer ")) ? "yes" : "no";

            if (status >= 400) {
                log.warn("[API DONE] [{}] {} {} -> {} ({}ms) user={} token={} ip={}", reqId, method, fullUri, status, duration, user, hasToken, ip);
            } else {
                log.info("[API DONE] [{}] {} {} -> {} ({}ms) user={} token={} ip={}", reqId, method, fullUri, status, duration, user, hasToken, ip);
            }
            // Penting: copy body ke response asli
            wrappedResponse.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip static / actuator jika ada
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/favicon.ico");
    }
}
