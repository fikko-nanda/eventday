package com.example.eventday.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Nama tidak boleh kosong")
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String name;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Username tidak boleh kosong")
    @Size(min = 3, max = 20, message = "Username 3-20 karakter")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username hanya boleh huruf, angka, dan underscore")
    private String username;

    @Size(max = 15, message = "Telepon maksimal 15 karakter")
    private String phone;

    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 6, message = "Password minimal 6 karakter")
    private String password;

    @Pattern(regexp = "\\d{16}", message = "NIK harus 16 digit angka")
    private String nik;

    private String role;
}
