package com.example.job_recommendation_backend.DTO;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String email;
    private String otp;
    private String name;
    private String password;
    private String role;

    public RegisterUserDto toRegisterUserDto() {
        return RegisterUserDto.builder()
                .name(name)
                .email(email)
                .password(password)
                .role(role)
                .build();
    }
}
