package com.example.eventday.service;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.entity.Auth;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.UserRepository;
import com.example.eventday.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar!");
        }

        if (request.getNik() != null && userRepository.existsByNik(request.getNik())) {
            throw new RuntimeException("NIK sudah terdaftar!");
        }

        User.Role roleEnum;
        try {
            String raw = request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER";
            roleEnum = User.Role.valueOf(raw);
        } catch (IllegalArgumentException e) {
            roleEnum = User.Role.CUSTOMER;
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .nik(request.getNik())
                .role(roleEnum)
                .build();

        User savedUser = userRepository.save(user);

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        Auth auth = Auth.builder()
                .user(savedUser)
                .password(hashedPassword)
                .status("INACTIVE")
                .build();
        authRepository.save(auth);

        auditLogService.log(savedUser.getUserId(), savedUser.getName(), "REGISTER",
                "Registrasi user baru: " + savedUser.getEmail());

        return AuthResponse.builder()
                .message("Registrasi berhasil!")
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email atau password salah!"));

        Auth auth = authRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Akun belum memiliki password, silakan hubungi admin"));

        if (!passwordEncoder.matches(request.getPassword(), auth.getPassword())) {
            auditLogService.log(user.getUserId(), user.getName(), "LOGIN_FAILED",
                    "Percobaan login gagal: " + request.getEmail());
            throw new RuntimeException("Email atau password salah!");
        }

        String roleName = user.getRole().name();
        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), roleName);
        LocalDateTime expiredToken = LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationMs() / 1000);

        auth.setAksesToken(token);
        auth.setExpiredToken(expiredToken);
        auth.setStatus("ACTIVE");
        auth.setUpdatedAt(LocalDateTime.now());
        authRepository.save(auth);

        auditLogService.log(user.getUserId(), user.getName(), "LOGIN",
                "Login sukses: " + user.getEmail());

        return AuthResponse.builder()
                .message("Login berhasil!")
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .token(token)
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .build();
    }

    @Transactional
    public void logout(UUID userId) {
        authRepository.findByUserUserId(userId).ifPresent(auth -> {
            auth.setAksesToken(null);
            auth.setExpiredToken(null);
            auth.setStatus("INACTIVE");
            auth.setUpdatedAt(LocalDateTime.now());
            authRepository.save(auth);
        });
    }
}
