package com.example.eventday.service;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.entity.User;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar!");
        }

        if (request.getNik() != null && userRepository.existsByNik(request.getNik())) {
            throw new RuntimeException("NIK sudah terdaftar!");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.Role.CUSTOMER)
                .nik(request.getNik())
                .build();

        User savedUser = userRepository.save(user);

        auditLogService.log(savedUser.getUserId(), savedUser.getName(), "REGISTER", "USER",
                savedUser.getUserId().toString(), "Registrasi user baru: " + savedUser.getEmail());

        return AuthResponse.builder()
                .message("Registrasi berhasil!")
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email atau password salah!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.log(user.getUserId(), user.getName(), "LOGIN_FAILED", "USER",
                    user.getUserId().toString(), "Percobaan login gagal: " + request.getEmail());
            throw new RuntimeException("Email atau password salah!");
        }

        auditLogService.log(user.getUserId(), user.getName(), "LOGIN", "USER",
                user.getUserId().toString(), "Login sukses: " + user.getEmail());

        return AuthResponse.builder()
                .message("Login berhasil!")
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
