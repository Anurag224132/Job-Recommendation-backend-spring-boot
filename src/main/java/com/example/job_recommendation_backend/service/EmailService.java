package com.example.job_recommendation_backend.service;

import java.time.LocalDateTime;

public interface EmailService {
    void sendVerificationEmail(String email, String name, String otp);
    void sendPasswordResetEmail(String email, String name, String otp);
    public void sendInterviewEmail(String email, String name, String jobTitle, LocalDateTime date, String link);
    public void sendContactUsEmail(String name, String email, String subject, String message);
    public void sendRejectionEmail(String email, String name, String jobTitle, String notes);
}
