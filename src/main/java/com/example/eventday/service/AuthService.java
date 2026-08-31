package com.example.eventday.service;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.entity.User;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public AuthResponse register(RegisterRequest request) {
        // 1. Validasi Email ganda
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar!");
        }

        // 2. Validasi NIK ganda (jika NIK diisi)
        if (request.getNik() != null && userRepository.existsByNik(request.getNik())) {
            throw new RuntimeException("NIK sudah terdaftar!");
        }

        // 3. Mapping data ke Entity User
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(request.getPassword())
                .role(request.getRole() != null ? request.getRole() : User.Role.CUSTOMER)
                .nik(request.getNik())
                .build();

        // 4. Simpan ke database (PostgreSQL akan men-generate UUID v4)
        User savedUser = userRepository.save(user);

        return AuthResponse.builder()
                .message("Registrasi berhasil!")
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Cari user berdasarkan email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email atau password salah!"));

        // 2. Validasi password
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Email atau password salah!");
        }

        return AuthResponse.builder()
                .message("Login berhasil!")
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}