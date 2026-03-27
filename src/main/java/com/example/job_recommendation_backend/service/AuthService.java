package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.LoginRequest;
import com.example.job_recommendation_backend.DTO.LoginResponse;
import com.example.job_recommendation_backend.DTO.RegisterUserDto;
import com.example.job_recommendation_backend.DTO.UserResponseDto;

public interface AuthService {

    void initiateRegistration(RegisterUserDto registerUserDto);
    String verifyAndRegister(String email, String otp, RegisterUserDto registerUserDto);
    LoginResponse login(LoginRequest loginRequest);
    UserResponseDto getCurrentUser(String email);
    void resendOtp(String email);
    void initiateForgotPassword(String email);
    void resetPassword(String email, String otp, String newPassword);
}
