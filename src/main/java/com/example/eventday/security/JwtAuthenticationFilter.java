package com.example.eventday.security;

import com.example.eventday.entity.Auth;
import com.example.eventday.repository.AuthRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRepository authRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (token == null) {
            log.debug("JWT: TIDAK ADA TOKEN -> tidak autentikasi (cek: Authorization header / cookie access_token dikirim? credentials:'include' active?)");
            filterChain.doFilter(request, response);
            return;
        }
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.debug("JWT: token ada tapi INVALID/EXPIRED -> tidak autentikasi");
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = jwtTokenProvider.getUserId(token);
            String role = jwtTokenProvider.getRole(token);

            Auth auth = authRepository.findByUserUserId(userId).orElse(null);
            if (auth == null || !"ACTIVE".equals(auth.getStatus()) || !token.equals(auth.getAksesToken())) {
                log.debug("JWT: token VALID tapi auth status/aksesToken TIDAK COCOK untuk user {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.debug("JWT: exception saat validasi token -> tidak autentikasi: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
