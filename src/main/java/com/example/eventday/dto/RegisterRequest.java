package com.example.eventday.dto;

import com.example.eventday.entity.User.Role;
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

    @NotBlank(message = "Telepon tidak boleh kosong")
    @Size(max = 20, message = "Telepon maksimal 20 karakter")
    private String phone;

    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 6, message = "Password minimal 6 karakter")
    private String password;

    private Role role;

    @Pattern(regexp = "\\d{16}", message = "NIK harus 16 digit angka")
    private String nik;
}
