package com.example.eventday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Nama tidak boleh kosong")
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String name;

    @Size(max = 15, message = "Nomor telepon maksimal 15 karakter")
    private String phone;

    @Pattern(regexp = "^\\d{16}$", message = "NIK harus 16 digit angka")
    private String nik;
}