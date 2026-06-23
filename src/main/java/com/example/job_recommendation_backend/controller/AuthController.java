package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.LoginRequest;
import com.example.job_recommendation_backend.DTO.LoginResponse;
import com.example.job_recommendation_backend.DTO.RegisterUserDto;
import com.example.job_recommendation_backend.DTO.VerifyOtpRequest;
import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.DTO.ResetPasswordRequest;
import com.example.job_recommendation_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterUserDto registerUserDto) {
        authService.initiateRegistration(registerUserDto);
        return ResponseEntity.ok(Map.of("nextStep", "verify-otp", "msg", "OTP sent successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        String token = authService.verifyAndRegister(
                verifyOtpRequest.getEmail(),
                verifyOtpRequest.getOtp(),
                verifyOtpRequest.toRegisterUserDto()
        );
        return ResponseEntity.ok(Map.of("token", token, "msg", "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @GetMapping("/user")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        String email = authentication.getName(); // The subject from JWT
        return ResponseEntity.ok(authService.getCurrentUser(email));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        authService.resendOtp(body.get("email"));
        return ResponseEntity.ok(Map.of("msg", "OTP resent successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        authService.initiateForgotPassword(body.get("email"));
        return ResponseEntity.ok(Map.of("msg", "Password reset OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("msg", "Password reset successful"));
    }
}
