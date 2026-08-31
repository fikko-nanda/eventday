package com.example.eventday.dto;

import com.example.eventday.entity.User.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String phone;
    private String password;
    private Role role;
    private String nik;
}