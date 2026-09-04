package com.example.eventday.service;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.GoogleLoginRequest;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.entity.Auth;
import com.example.eventday.entity.Otp;
import com.example.eventday.entity.PasswordResetToken;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.OtpRepository;
import com.example.eventday.repository.PasswordResetTokenRepository;
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
@SuppressWarnings("null")
public class AuthService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final OtpRepository otpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.reset-password.code-length:6}")
    private int resetCodeLength;

    @Value("${app.reset-password.expiry-minutes:15}")
    private int resetPasswordExpiryMinutes;

    @Value("${app.reset-password.frontend-url:}")
    private String frontendUrl;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar!");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username sudah terdaftar!");
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
                .username(request.getUsername())
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

        String otpCode = String.format("%0" + otpLength + "d", (int)(Math.random() * Math.pow(10, otpLength)));
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        Otp otp = Otp.builder()
                .user(savedUser)
                .otpCode(otpCode)
                .expiredAt(expiredAt)
                .build();
        otpRepository.save(otp);

        emailService.sendOtpEmail(savedUser.getEmail(), savedUser.getName(), otpCode);

        return AuthResponse.builder()
                .message("Registrasi berhasil! OTP telah dikirim ke email Anda.")
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Email atau username harus diisi!");
        }

        User user = null;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier).orElse(null);
            if (user == null) user = userRepository.findByUsername(identifier).orElse(null);
        } else {
            user = userRepository.findByUsername(identifier).orElse(null);
            if (user == null) user = userRepository.findByEmail(identifier).orElse(null);
        }
        if (user == null) {
            throw new RuntimeException("Email atau password salah!");
        }

        Auth auth = authRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Akun belum memiliki password, silakan hubungi admin"));

        if (!passwordEncoder.matches(request.getPassword(), auth.getPassword())) {
            auditLogService.log(user.getUserId(), user.getName(), "LOGIN_FAILED",
                    "Percobaan login gagal: " + request.getEmail());
            throw new RuntimeException("Email atau password salah!");
        }

        if ("INACTIVE".equals(auth.getStatus())) {
            throw new RuntimeException("Akun belum aktif! Silakan verifikasi OTP terlebih dahulu.");
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
                .username(user.getUsername())
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

        Auth auth = authRepository.findByUserUserId(user.getUserId()).orElse(null);
        if (auth == null) {
            String dummyPass = passwordEncoder.encode(UUID.randomUUID().toString());
            auth = Auth.builder()
                    .user(user)
                    .password(dummyPass)
                    .authGoogle(googleId.length() > 20 ? googleId.substring(0, 20) : googleId)
                    .status("INACTIVE")
                    .build();
        } else {
            String shortId = googleId.length() > 20 ? googleId.substring(0, 20) : googleId;
            auth.setAuthGoogle(shortId);
        }

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
                .username(user.getUsername())
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
            if (googleClientId != null && !googleClientId.isBlank()) {
                String aud = (String) res.get("aud");
                if (!googleClientId.equals(aud)) {
                    throw new RuntimeException("Token Google aud tidak sesuai dengan google.client-id");
                }
            }
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

    @Transactional
    public void verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));

        Auth auth = authRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Akun tidak ditemukan!"));

        Otp otp = otpRepository.findByUserUserIdAndOtpCode(user.getUserId(), otpCode)
                .orElseThrow(() -> new RuntimeException("Kode OTP tidak valid!"));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otp);
            throw new RuntimeException("Kode OTP sudah expired!");
        }

        otpRepository.delete(otp);

        auth.setStatus("ACTIVE");
        auth.setUpdatedAt(LocalDateTime.now());
        authRepository.save(auth);

        auditLogService.log(user.getUserId(), user.getName(), "VERIFY_OTP_SUCCESS",
                "Verifikasi OTP berhasil: " + user.getEmail());
    }

    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));

        otpRepository.deleteByUserUserId(user.getUserId());

        String otpCode = String.format("%0" + otpLength + "d", (int)(Math.random() * Math.pow(10, otpLength)));
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        Otp otp = Otp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiredAt(expiredAt)
                .build();
        otpRepository.save(otp);

        emailService.sendOtpEmail(user.getEmail(), user.getName(), otpCode);

        auditLogService.log(user.getUserId(), user.getName(), "OTP_RESENT",
                "OTP di-resend ke: " + user.getEmail());
    }

    @Transactional
    public void sendResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));

        passwordResetTokenRepository.deleteByUserUserId(user.getUserId());

        String resetCode = String.format("%0" + resetCodeLength + "d", (int)(Math.random() * Math.pow(10, resetCodeLength)));
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(resetPasswordExpiryMinutes);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(resetCode)
                .expiredAt(expiredAt)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Kirim OTP (bukan link) agar frontend bisa: input email → terima OTP → klik Lanjutkan → verifikasi → halaman new password
        emailService.sendResetOtpEmail(user.getEmail(), user.getName(), resetCode, resetPasswordExpiryMinutes);

        auditLogService.log(user.getUserId(), user.getName(), "PASSWORD_RESET_TOKEN_SENT",
                "Reset password OTP dikirim ke: " + user.getEmail());
    }

    @Transactional(readOnly = true)
    public void verifyResetCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));

        PasswordResetToken resetTokenEntity = passwordResetTokenRepository.findByUserUserIdAndToken(user.getUserId(), code)
                .orElseThrow(() -> new RuntimeException("Kode OTP tidak valid!"));

        if (resetTokenEntity.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Kode OTP sudah expired!");
        }
        // tidak delete token di sini — biarkan resetPassword yang delete setelah sukses ganti password
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));

        PasswordResetToken resetTokenEntity = passwordResetTokenRepository.findByUserUserIdAndToken(user.getUserId(), code)
                .orElseThrow(() -> new RuntimeException("Kode reset password tidak valid!"));

        if (resetTokenEntity.getExpiredAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetTokenEntity);
            throw new RuntimeException("Kode reset password sudah expired!");
        }

        Auth auth = authRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Akun tidak ditemukan!"));

        auth.setPassword(passwordEncoder.encode(newPassword));
        auth.setUpdatedAt(LocalDateTime.now());
        authRepository.save(auth);

        passwordResetTokenRepository.delete(resetTokenEntity);

        auditLogService.log(user.getUserId(), user.getName(), "PASSWORD_RESET_SUCCESS",
                "Password berhasil direset untuk: " + user.getEmail());
    }
}
