package com.example.job_recommendation_backend.service;

public interface EmailService {
    void sendVerificationEmail(String email, String name, String otp);
    void sendPasswordResetEmail(String email, String name, String otp);
}
