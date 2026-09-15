package com.example.eventday.service;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.GoogleLoginRequest;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.dto.ResetPasswordRequest;
import com.example.eventday.entity.Auth;
import com.example.eventday.entity.Otp;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.OtpRepository;
import com.example.eventday.repository.UserRepository;
import com.example.eventday.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.reset-password.expiry-minutes:15}")
    private int resetExpiryMinutes;

    @Value("${app.reset-password.code-length:6}")
    private int resetCodeLength;

    private final SecureRandom secureRandom = new SecureRandom();

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

        // Register publik selalu CUSTOMER — cegah eskalasi privilege via role=ADMIN/ORGANIZER.
        // Akun ADMIN/ORGANIZER dibuat lewat jalur internal (DB/endpoint admin), bukan register publik.
        User.Role roleEnum = User.Role.CUSTOMER;

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

        String otpCode = generateCode(otpLength);
        otpRepository.deleteByUserUserId(savedUser.getUserId());
        Otp otp = Otp.builder()
                .user(savedUser)
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();
        otpRepository.save(otp);
        emailService.sendOtpEmail(savedUser.getEmail(), otpCode);

        auditLogService.log(savedUser.getUserId(), savedUser.getName(), "REGISTER",
                "Registrasi user baru: " + savedUser.getEmail());

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
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Email atau password salah!");
        }

        User user = resolveUserByIdentifier(identifier);
        if (user == null) {
            throw new RuntimeException("Email atau password salah!");
        }

        Auth auth = authRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Akun belum memiliki password, silakan hubungi admin"));

        if (!passwordEncoder.matches(request.getPassword(), auth.getPassword())) {
            auditLogService.log(user.getUserId(), user.getName(), "LOGIN_FAILED",
                    "Percobaan login gagal: " + identifier);
            throw new RuntimeException("Email atau password salah!");
        }

        if (!"ACTIVE".equalsIgnoreCase(auth.getStatus())) {
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

    private User resolveUserByIdentifier(String identifier) {
        boolean isEmail = identifier.contains("@");
        if (isEmail) {
            User byEmail = userRepository.findByEmail(identifier).orElse(null);
            if (byEmail != null) return byEmail;
            return userRepository.findByUsername(identifier).orElse(null);
        } else {
            User byUsername = userRepository.findByUsername(identifier).orElse(null);
            if (byUsername != null) return byUsername;
            return userRepository.findByEmail(identifier).orElse(null);
        }
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
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
            if (baseUsername.length() < 3) baseUsername = baseUsername + "_user";
            if (baseUsername.length() > 20) baseUsername = baseUsername.substring(0, 20);
            String username = baseUsername;
            int suffix = 1;
            while (userRepository.existsByUsername(username)) {
                String suf = String.valueOf(suffix++);
                username = baseUsername.substring(0, Math.min(baseUsername.length(), 20 - suf.length())) + suf;
            }
            user = User.builder()
                    .name(name)
                    .email(email)
                    .username(username)
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

    @Transactional
    public String verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));
        Otp otp = otpRepository.findByUserUserIdAndOtpCode(user.getUserId(), otpCode)
                .orElseThrow(() -> new RuntimeException("Kode OTP tidak valid!"));
        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Kode OTP sudah expired!");
        }
        Auth auth = authRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Auth tidak ditemukan!"));
        auth.setStatus("ACTIVE");
        auth.setUpdatedAt(LocalDateTime.now());
        authRepository.save(auth);
        otpRepository.deleteByUserUserId(user.getUserId());
        auditLogService.log(user.getUserId(), user.getName(), "VERIFY_OTP", "OTP terverifikasi: " + email);
        return "OTP terverifikasi! Akun aktif, silakan login.";
    }

    @Transactional
    public String resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));
        otpRepository.deleteByUserUserId(user.getUserId());
        String otpCode = generateCode(otpLength);
        Otp otp = Otp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();
        otpRepository.save(otp);
        emailService.sendOtpEmail(user.getEmail(), otpCode);
        auditLogService.log(user.getUserId(), user.getName(), "RESEND_OTP", "Resend OTP: " + email);
        return "OTP baru berhasil dikirim ke email Anda!";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email tidak boleh kosong");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan!"));

        String effectiveCode = request.getEffectiveCode();
        String effectiveNewPassword = request.getEffectiveNewPassword();

        Auth auth = authRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Auth tidak ditemukan!"));

        if (effectiveCode == null || effectiveCode.isBlank()) {
            if (effectiveNewPassword != null && !effectiveNewPassword.isBlank()) {
                throw new RuntimeException("Kode reset password tidak valid/expired");
            }
            String code = generateCode(resetCodeLength);
            auth.setResetToken(code);
            auth.setResetExpiredAt(LocalDateTime.now().plusMinutes(resetExpiryMinutes));
            auth.setUpdatedAt(LocalDateTime.now());
            authRepository.save(auth);
            emailService.sendResetPasswordEmail(user.getEmail(), code);
            auditLogService.log(user.getUserId(), user.getName(), "RESET_PASSWORD_REQUEST", "Minta kode reset: " + user.getEmail());
            return "Kode reset password berhasil dikirim ke email Anda!";
        }

        if (effectiveNewPassword == null || effectiveNewPassword.length() < 6) {
            throw new RuntimeException("Password baru minimal 6 karakter");
        }
        if (auth.getResetToken() == null || !auth.getResetToken().equals(effectiveCode)) {
            throw new RuntimeException("Kode reset password tidak valid/expired");
        }
        if (auth.getResetExpiredAt() == null || auth.getResetExpiredAt().isBefore(LocalDateTime.now())) {
            auth.setResetToken(null);
            auth.setResetExpiredAt(null);
            authRepository.save(auth);
            throw new RuntimeException("Kode reset password tidak valid/expired");
        }
        auth.setPassword(passwordEncoder.encode(effectiveNewPassword));
        auth.setResetToken(null);
        auth.setResetExpiredAt(null);
        auth.setUpdatedAt(LocalDateTime.now());
        authRepository.save(auth);
        auditLogService.log(user.getUserId(), user.getName(), "PASSWORD_RESET_SUCCESS", "Password direset: " + user.getEmail());
        return "Password berhasil direset! Silakan login dengan password baru.";
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
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
            // Cukup null-kan token: JwtAuthenticationFilter menolak token yang
            // tidak sama dengan auth.aksesToken, jadi token lama langsung mati.
            // Status TIDAK diubah agar user tetap bisa login kembali.
            auth.setAksesToken(null);
            auth.setExpiredToken(null);
            auth.setUpdatedAt(LocalDateTime.now());
            authRepository.save(auth);
        });
    }
}
