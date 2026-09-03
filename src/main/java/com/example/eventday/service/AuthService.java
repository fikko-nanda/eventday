package com.example.eventday.service;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.GoogleLoginRequest;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.entity.Auth;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.UserRepository;
import com.example.eventday.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    @Value("${google.client-id:}")
    private String googleClientId;

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
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        Map<String, Object> payload = verifyGoogleIdToken(request.getIdToken());

        String googleId = (String) payload.get("sub");
        String email = (String) payload.get("email");
        String name = (String) payload.getOrDefault("name", email);
        Boolean emailVerified = Boolean.valueOf(String.valueOf(payload.getOrDefault("email_verified", "true")));

        if (email == null || googleId == null) {
            throw new RuntimeException("Token Google tidak valid: email/sub hilang");
        }
        if (!emailVerified) {
            throw new RuntimeException("Email Google belum terverifikasi");
        }

        // 1. Cari atau buat User
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = false;
        if (user == null) {
            user = User.builder()
                    .name(name)
                    .email(email)
                    .role(User.Role.CUSTOMER)
                    .build();
            user = userRepository.save(user);
            isNewUser = true;
        }

        // 2. Cari atau buat Auth
        Auth auth = authRepository.findByUserUserId(user.getUserId()).orElse(null);
        if (auth == null) {
            // password dummy karena kolom NOT NULL — tidak dipakai untuk login Google
            String dummyPass = passwordEncoder.encode(UUID.randomUUID().toString());
            auth = Auth.builder()
                    .user(user)
                    .password(dummyPass)
                    .authGoogle(googleId.length() > 20 ? googleId.substring(0, 20) : googleId)
                    .status("INACTIVE")
                    .build();
        } else {
            // update google id jika belum ada atau berubah (potong 20 char sesuai DDL)
            String shortId = googleId.length() > 20 ? googleId.substring(0, 20) : googleId;
            auth.setAuthGoogle(shortId);
        }

        // 3. Generate JWT session sama seperti login biasa
        String roleName = user.getRole().name();
        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), roleName);
        auth.setAksesToken(token);
        auth.setExpiredToken(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationMs() / 1000));
        auth.setStatus("ACTIVE");
        auth.setUpdatedAt(LocalDateTime.now());
        authRepository.save(auth);

        auditLogService.log(user.getUserId(), user.getName(), isNewUser ? "REGISTER_GOOGLE" : "LOGIN_GOOGLE",
                (isNewUser ? "Registrasi via Google: " : "Login via Google: ") + email);

        return AuthResponse.builder()
                .message(isNewUser ? "Registrasi via Google berhasil!" : "Login via Google berhasil!")
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .token(token)
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        try {
            RestTemplate rest = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl("https://oauth2.googleapis.com/tokeninfo")
                    .queryParam("id_token", idToken)
                    .toUriString();
            Map<String, Object> res = rest.getForObject(url, Map.class);
            if (res == null || res.containsKey("error_description")) {
                throw new RuntimeException("Token Google tidak valid: " + (res != null ? res.get("error_description") : "empty"));
            }
            // validasi aud jika google.client-id diisi
            if (googleClientId != null && !googleClientId.isBlank()) {
                String aud = (String) res.get("aud");
                if (!googleClientId.equals(aud)) {
                    throw new RuntimeException("Token Google aud tidak sesuai dengan google.client-id");
                }
            }
            // cek exp
            String expStr = String.valueOf(res.get("exp"));
            long exp = Long.parseLong(expStr);
            if (exp * 1000 < System.currentTimeMillis()) {
                throw new RuntimeException("Token Google sudah expired");
            }
            return res;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gagal verifikasi token Google: " + e.getMessage());
        }
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
