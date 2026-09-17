package com.example.eventday;

import com.example.eventday.dto.AuthResponse;
import com.example.eventday.dto.LoginRequest;
import com.example.eventday.dto.RegisterRequest;
import com.example.eventday.dto.ResetPasswordRequest;
import com.example.eventday.entity.Auth;
import com.example.eventday.entity.Otp;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.OtpRepository;
import com.example.eventday.repository.UserRepository;
import com.example.eventday.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired AuthRepository authRepository;
    @Autowired OtpRepository otpRepository;

    private String uniqueEmail() { return "test_" + UUID.randomUUID().toString().substring(0,8) + "@mail.com"; }
    private String uniqueUsername() { return "user_" + UUID.randomUUID().toString().substring(0,6); }
    private String uniqueNik() { return String.format("%016d", (long)(Math.random()*1_000_000_000_000_0000L) % 9000000000000000L + 1000000000000000L); }

    private AuthResponse registerOk(String email, String username, String nik) {
        RegisterRequest r = new RegisterRequest();
        r.setName("Tester");
        r.setEmail(email);
        r.setUsername(username);
        r.setPassword("secret123");
        r.setNik(nik);
        r.setRole("CUSTOMER");
        return authService.register(r);
    }

    @Test
    void register_success_createsInactiveUserAndOtp() throws Exception {
        String email = uniqueEmail(); String username = uniqueUsername(); String nik = uniqueNik();
        AuthResponse resp = registerOk(email, username, nik);
        assertEquals("Registrasi berhasil! OTP telah dikirim ke email Anda.", resp.getMessage());
        assertEquals(email, resp.getEmail());
        assertEquals(username, resp.getUsername());
        assertNull(resp.getToken());
        User u = userRepository.findByEmail(email).orElseThrow();
        assertEquals("CUSTOMER", u.getRole().name());
        assertTrue(otpRepository.findByUserUserId(u.getUserId()).isPresent());
        assertEquals("INACTIVE", authRepository.findByUserUserId(u.getUserId()).orElseThrow().getStatus());
    }

    @Test
    void register_duplicateEmail_throws() {
        String email = uniqueEmail(); String u1 = uniqueUsername(); String u2 = uniqueUsername();
        registerOk(email, u1, uniqueNik());
        RegisterRequest r = new RegisterRequest();
        r.setName("Dup"); r.setEmail(email); r.setUsername(u2); r.setPassword("secret123"); r.setNik(uniqueNik());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(r));
        assertThat(ex.getMessage()).contains("Email sudah terdaftar");
    }

    @Test
    void register_duplicateUsername_throws() {
        String email1 = uniqueEmail(); String email2 = uniqueEmail(); String username = uniqueUsername();
        registerOk(email1, username, uniqueNik());
        RegisterRequest r = new RegisterRequest();
        r.setName("Dup"); r.setEmail(email2); r.setUsername(username); r.setPassword("secret123"); r.setNik(uniqueNik());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(r));
        assertThat(ex.getMessage()).contains("Username sudah terdaftar");
    }

    @Test
    void register_duplicateNik_throws() {
        String nik = uniqueNik();
        registerOk(uniqueEmail(), uniqueUsername(), nik);
        RegisterRequest r = new RegisterRequest();
        r.setName("Dup"); r.setEmail(uniqueEmail()); r.setUsername(uniqueUsername()); r.setPassword("secret123"); r.setNik(nik);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(r));
        assertThat(ex.getMessage()).contains("NIK sudah terdaftar");
    }

    @Test
    void register_viaMockMvc_validationUsernameRequired() throws Exception {
        String json = "{\"name\":\"John\",\"email\":\"john@mail.com\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_viaMockMvc_invalidUsernamePattern() throws Exception {
        String json = String.format("{\"name\":\"John\",\"email\":\"%s\",\"username\":\"bad-name!\",\"password\":\"secret123\"}", uniqueEmail());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_fails_whenInactive() {
        String email = uniqueEmail(); String username = uniqueUsername();
        registerOk(email, username, uniqueNik());
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword("secret123");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertThat(ex.getMessage()).contains("belum aktif");
    }

    @Test
    void verifyOtp_success_activatesAccount() {
        String email = uniqueEmail(); String username = uniqueUsername();
        registerOk(email, username, uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        Otp otp = otpRepository.findByUserUserId(u.getUserId()).orElseThrow();
        String msg = authService.verifyOtp(email, otp.getOtpCode());
        assertThat(msg).contains("OTP terverifikasi");
        assertEquals("ACTIVE", authRepository.findByUserUserId(u.getUserId()).orElseThrow().getStatus());
        assertTrue(otpRepository.findByUserUserId(u.getUserId()).isEmpty());
    }

    @Test
    void verifyOtp_invalidCode_throws() {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.verifyOtp(email, "000000"));
        assertThat(ex.getMessage()).contains("tidak valid");
    }

    @Test
    void verifyOtp_expired_throws() {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        Otp otp = otpRepository.findByUserUserId(u.getUserId()).orElseThrow();
        otp.setExpiredAt(LocalDateTime.now().minusMinutes(1));
        otpRepository.save(otp);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.verifyOtp(email, otp.getOtpCode()));
        assertThat(ex.getMessage()).contains("expired");
    }

    @Test
    void login_success_viaEmail_andUsername_andIdentifier() {
        String email = uniqueEmail(); String username = uniqueUsername();
        registerOk(email, username, uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        Otp otp = otpRepository.findByUserUserId(u.getUserId()).orElseThrow();
        authService.verifyOtp(email, otp.getOtpCode());

        LoginRequest byEmail = new LoginRequest(); byEmail.setEmail(email); byEmail.setPassword("secret123");
        AuthResponse r1 = authService.login(byEmail);
        assertNotNull(r1.getToken()); assertTrue(r1.getExpiresIn() > 0);

        LoginRequest byUsername = new LoginRequest(); byUsername.setUsername(username); byUsername.setPassword("secret123");
        AuthResponse r2 = authService.login(byUsername);
        assertNotNull(r2.getToken());

        LoginRequest byId = new LoginRequest(); byId.setIdentifier(username); byId.setPassword("secret123");
        AuthResponse r3 = authService.login(byId);
        assertNotNull(r3.getToken());

        LoginRequest idEmail = new LoginRequest(); idEmail.setIdentifier(email); idEmail.setPassword("secret123");
        assertNotNull(authService.login(idEmail).getToken());
    }

    @Test
    void login_wrongPassword_throws() {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        authService.verifyOtp(email, otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode());
        LoginRequest bad = new LoginRequest(); bad.setEmail(email); bad.setPassword("wrongpass");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(bad));
        assertThat(ex.getMessage()).contains("Email atau password salah");
    }

    @Test
    void login_viaMockMvc_success() throws Exception {
        String email = uniqueEmail(); String username = uniqueUsername();
        registerOk(email, username, uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        authService.verifyOtp(email, otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode());
        String json = String.format("{\"identifier\":\"%s\",\"password\":\"secret123\"}", username);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().httpOnly("access_token", true));
    }

    @Test
    void resendOtp_generatesNewCode() {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        String oldCode = otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode();
        String msg = authService.resendOtp(email);
        assertThat(msg).contains("OTP baru");
        String newCode = otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode();
        assertNotEquals(oldCode, newCode);
    }

    @Test
    void resendOtp_viaMockMvc() throws Exception {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        String json = String.format("{\"email\":\"%s\"}", email);
        mockMvc.perform(post("/api/auth/resend-otp").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("OTP baru berhasil dikirim ke email Anda!"))
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void verifyOtp_viaMockMvc() throws Exception {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        String code = otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode();
        String json = String.format("{\"email\":\"%s\",\"otpCode\":\"%s\"}", email, code);
        mockMvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("OTP terverifikasi! Akun aktif, silakan login."))
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void resetPassword_flow_phase1_and_phase2_success() {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        authService.verifyOtp(email, otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode());

        ResetPasswordRequest req1 = new ResetPasswordRequest();
        req1.setEmail(email);
        String msg1 = authService.resetPassword(req1);
        assertThat(msg1).contains("Kode reset");
        Auth authWithToken = authRepository.findByUserUserId(u.getUserId()).orElseThrow();
        String code = authWithToken.getResetToken();
        assertNotNull(code); assertEquals(6, code.length());

        ResetPasswordRequest req2 = new ResetPasswordRequest();
        req2.setEmail(email); req2.setCode(code); req2.setNewPassword("newSecret123");
        String msg2 = authService.resetPassword(req2);
        assertThat(msg2).contains("Password berhasil direset");

        LoginRequest login = new LoginRequest(); login.setEmail(email); login.setPassword("newSecret123");
        AuthResponse ok = authService.login(login);
        assertNotNull(ok.getToken());

        LoginRequest old = new LoginRequest(); old.setEmail(email); old.setPassword("secret123");
        assertThrows(RuntimeException.class, () -> authService.login(old));
    }

    @Test
    void resetPassword_invalidCode_throws() {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        authService.verifyOtp(email, otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode());
        ResetPasswordRequest r1 = new ResetPasswordRequest(); r1.setEmail(email);
        authService.resetPassword(r1);
        ResetPasswordRequest r2 = new ResetPasswordRequest(); r2.setEmail(email); r2.setCode("999999"); r2.setNewPassword("newpass123");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.resetPassword(r2));
        assertThat(ex.getMessage()).contains("tidak valid");
    }

    @Test
    void resetPassword_viaMockMvc_bothPhases() throws Exception {
        String email = uniqueEmail();
        registerOk(email, uniqueUsername(), uniqueNik());
        User u = userRepository.findByEmail(email).orElseThrow();
        authService.verifyOtp(email, otpRepository.findByUserUserId(u.getUserId()).orElseThrow().getOtpCode());

        String json1 = String.format("{\"email\":\"%s\"}", email);
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("Kode reset password berhasil dikirim ke email Anda!"))
                .andExpect(jsonPath("$.status").value(200));

        String code = authRepository.findByUserUserId(u.getUserId()).orElseThrow().getResetToken();

        String json2 = String.format("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"mockNew123\"}", email, code);
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("Password berhasil direset! Silakan login dengan password baru."))
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void googleLogin_invalidToken_fails() {
        com.example.eventday.dto.GoogleLoginRequest gl = new com.example.eventday.dto.GoogleLoginRequest();
        gl.setIdToken("invalid.token.here");
        assertThrows(RuntimeException.class, () -> authService.loginWithGoogle(gl));
    }

    @Test
    void jwt_filter_protects_endpoints() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"nouser\",\"password\":\"nopass\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.msg").isNotEmpty());
    }
}
