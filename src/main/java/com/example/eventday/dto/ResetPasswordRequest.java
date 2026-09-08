package com.example.eventday.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;

    @JsonAlias({"token", "otp"})
    private String code;

    private String token;

    @JsonAlias({"newPassword", "password", "new_password"})
    private String newPassword;

    public String getEffectiveCode() {
        if (code != null && !code.isBlank()) return code.trim();
        if (token != null && !token.isBlank()) return token.trim();
        return null;
    }

    public String getEffectiveNewPassword() {
        return newPassword != null ? newPassword.trim() : null;
    }
}
