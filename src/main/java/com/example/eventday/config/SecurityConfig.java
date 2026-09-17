package com.example.eventday.config;

import com.example.eventday.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.eventday.dto.ApiResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ObjectMapper om = new ObjectMapper();
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ApiResponse<Void> body = ApiResponse.unauthorized("Unauthorized: token tidak ada atau tidak valid");
                    res.getWriter().write(om.writeValueAsString(body));
                })
                .accessDeniedHandler((req, res, accessEx) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ApiResponse<Void> body = ApiResponse.forbidden("Forbidden: akses ditolak");
                    res.getWriter().write(om.writeValueAsString(body));
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Root & error ngrok
                .requestMatchers("/", "/error").permitAll()

                // Modul Auth (Login, Register, OTP, Google)
                .requestMatchers("/api/auth/**").permitAll()

                // Modul 01: Home & Search (Publik)
                .requestMatchers("/api/home/**").permitAll()
                .requestMatchers("/api/search/**").permitAll()

                // Modul 02: Katalog Event Publik
                .requestMatchers("/api/events/**").permitAll()

                // Modul Legal & Informasi Statis (Publik) — dual alias root + /api (LegalController)
                .requestMatchers("/terms-conditions", "/privacy-policy", "/api/terms-conditions", "/api/privacy-policy").permitAll()

                // Webhook Midtrans — harus publik (Midtrans server tidak punya JWT)
                .requestMatchers("/api/payments/midtrans-notification").permitAll()

                // Modul Admin/Superadmin — hanya ROLE_ADMIN (filter set ROLE_<role> dari JWT) — dual alias /admin + /api/admin
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                // Semua endpoint lain (Checkout, Payment, My Tickets, Profile, Organizer) wajib login
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
}