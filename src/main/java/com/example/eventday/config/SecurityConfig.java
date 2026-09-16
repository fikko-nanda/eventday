package com.example.eventday.config;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                .requestMatchers("/api/v1/auth/**", "/api/auth/**").permitAll()

                // Modul Publik
                .requestMatchers("/api/v1/home/**", "/api/home/**").permitAll()
                .requestMatchers("/api/v1/search/**", "/api/search/**").permitAll()
                .requestMatchers("/api/v1/events/**", "/api/events/**").permitAll()

                // Modul Legal & Informasi Statis (Publik)
                .requestMatchers("/terms-conditions", "/privacy-policy", "/api/v1/terms-conditions", "/api/v1/privacy-policy").permitAll()

                // Webhook Midtrans — publik untuk route /api dan /api/v1
                .requestMatchers("/api/payments/midtrans-notification", "/api/v1/payments/midtrans-notification").permitAll()

                // Modul Admin
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Endpoint pembayaran & selebihnya wajib login (termasuk /api/payments/charge & /api/v1/payments/charge)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}