package com.example.eventday;

import com.example.eventday.dto.*;
import com.example.eventday.entity.*;
import com.example.eventday.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
public class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthRepository authRepository;
    @Autowired private OtpRepository otpRepository;
    @Autowired private PasswordResetTokenRepository resetRepo;

    @BeforeEach
    void cleanup() {
        userRepository.findByEmail("test_end2end@mail.com").ifPresent(user -> {
            otpRepository.deleteByUserUserId(user.getUserId());
            resetRepo.deleteByUserUserId(user.getUserId());
            authRepository.findByUserUserId(user.getUserId()).ifPresent(authRepository::delete);
            userRepository.delete(user);
        });
    }

    @Test
    void endToEndAuthenticationFlow() throws Exception {
        String email = "test_end2end@mail.com";
        String password = "password123";

        // 1. REGISTER
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Test User");
        reg.setEmail(email);
        reg.setUsername("testuser123");
        reg.setPassword(password);
        reg.setRole("CUSTOMER");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());
        
        User user = userRepository.findByEmail(email).orElseThrow();

        // 2. LOGIN (Harus GAGAL karena akun masih INACTIVE belum OTP)
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Akun belum aktif! Silakan verifikasi OTP terlebih dahulu."));

        // 3. RESEND OTP
        ResendOtpRequest resend = new ResendOtpRequest();
        resend.setEmail(email);
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resend)))
                .andExpect(status().isOk());

        // Ambil OTP dari DB
        Otp otp = otpRepository.findAll().stream()
                .filter(o -> o.getUser().getUserId().equals(user.getUserId()))
                .findFirst().orElseThrow();

        // 4. VERIFY OTP
        VerifyOtpRequest verify = new VerifyOtpRequest();
        verify.setEmail(email);
        verify.setOtpCode(otp.getOtpCode());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk());

        // 5. LOGIN (Harus SUKSES karena akun sudah ACTIVE)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // 6. FORGOT PASSWORD (Minta Kode)
        ResetPasswordRequest forgot = new ResetPasswordRequest();
        forgot.setEmail(email);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgot)))
                .andExpect(status().isOk());

        // Ambil Kode Reset dari DB
        PasswordResetToken resetToken = resetRepo.findAll().stream()
                .filter(t -> t.getUser().getUserId().equals(user.getUserId()))
                .findFirst().orElseThrow();

        // 7. RESET PASSWORD (Submit Kode + Password Baru)
        String newPassword = "newPassword123";
        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setEmail(email);
        reset.setCode(resetToken.getToken());
        reset.setNewPassword(newPassword);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isOk());

        // 8. LOGIN DENGAN PASSWORD BARU (SUKSES)
        login.setPassword(newPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
        
        System.out.println("===============================================");
        System.out.println(">>> SEMUA SKENARIO UJI COBA SUKSES 100% <<<");
        System.out.println("===============================================");
    }
}
