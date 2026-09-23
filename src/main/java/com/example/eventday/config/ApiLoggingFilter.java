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
@SuppressWarnings("null")
public class ApiLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String reqId = UUID.randomUUID().toString().substring(0, 8);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        // 1. Ambil IP Asli Client (Bypass Proxy Ngrok/Nginx)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        String userAgent = request.getHeader("User-Agent");
        String fullUri = query != null ? uri + "?" + query : uri;

        // 2. Wrap Response agar status code dan body dapat dibaca di akhir
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        String shortUa = userAgent != null ? userAgent.substring(0, Math.min(40, userAgent.length())).replaceAll("\\s+", " ") : "-";

        try {
            // Log Request Masuk (Gunakan Teks ASCII Karakter Bersih)
            log.info("[IN]  [{}] {} {} | IP={} | UA={}", reqId, method, fullUri, ip, shortUa);
            chain.doFilter(request, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = wrappedResponse.getStatus();

            String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "-";
            String authHeader = request.getHeader("Authorization");
            boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");
            String cookie = request.getHeader("Cookie");
            boolean hasCookie = cookie != null && cookie.contains("access_token");

            // 3. Format Penanda Status Tanpa Emoji Agar Tidak Rusak di Terminal
            String statusTag;
            String statusLabel;
            if (status >= 500) {
                statusTag = "[ERROR]";
                statusLabel = "ERROR";
            } else if (status >= 400) {
                statusTag = "[WARN]";
                statusLabel = "FAIL";
            } else if (status >= 300) {
                statusTag = "[INFO]";
                statusLabel = "REDIRECT";
            } else {
                statusTag = "[OK]";
                statusLabel = "OK";
            }

            String authInfo = hasToken ? "Bearer" : hasCookie ? "Cookie" : "none";
            String durationStr = duration > 1000 ? String.format("%.2fs", duration / 1000.0) : duration + "ms";
            String slowNotice = duration > 1000 ? " [SLOW!]" : "";

            // 4. Catat Log Outgoing Sesuai Level Status
            if (status >= 500) {
                log.error("[OUT] {} [{}] {} {} -> {} {} ({}) user={} auth={} ip={}{}", 
                        statusTag, reqId, method, fullUri, status, statusLabel, durationStr, user, authInfo, ip, slowNotice);
            } else if (status >= 400) {
                log.warn("[OUT] {} [{}] {} {} -> {} {} ({}) user={} auth={} ip={}{}", 
                        statusTag, reqId, method, fullUri, status, statusLabel, durationStr, user, authInfo, ip, slowNotice);
            } else {
                log.info("[OUT] {} [{}] {} {} -> {} {} ({}) user={} auth={} ip={}{}", 
                        statusTag, reqId, method, fullUri, status, statusLabel, durationStr, user, authInfo, ip, slowNotice);
            }

            // 5. Salin Kembali Body Response Agar Diterima Frontend
            wrappedResponse.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Abaikan request preflight OPTIONS agar tidak mengotori log
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/favicon.ico");
    }
}