package com.example.job_recommendation_backend.service.impl;

import com.example.job_recommendation_backend.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendVerificationEmail(String email, String name, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Verify Your Email Address");

            String htmlContent = String.format(
                    "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                            "    <h2 style=\"color: #2563eb;\">Email Verification</h2>" +
                            "    <p>Hello %s,</p>" +
                            "    <p>Thank you for registering. Please use the following OTP to verify your email address:</p>" +
                            "    <div style=\"background: #f4f4f4; padding: 10px; margin: 20px 0; text-align: center; font-size: 24px; letter-spacing: 5px;\">" +
                            "        %s" +
                            "    </div>" +
                            "    <p>This OTP is valid for 15 minutes.</p>" +
                            "    <p>If you didn't request this, please ignore this email.</p>" +
                            "    <p>Best regards,<br>Your App Team</p>" +
                            "</div>", name, otp);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Error sending verification email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String email, String name, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Password Reset OTP");

            String htmlContent = String.format(
                    "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                        "    <h2 style=\"color: #2563eb;\">Password Reset Request</h2>" +
                        "    <p>Hello %s,</p>" +
                        "    <p>We received a request to reset your password. Please use the following OTP to complete the process:</p>" +
                        "    <div style=\"background: #f4f4f4; padding: 10px; margin: 20px 0; text-align: center; font-size: 24px; letter-spacing: 5px;\">" +
                        "        %s" +
                        "    </div>" +
                        "    <p>This OTP is valid for 15 minutes.</p>" +
                        "    <p>If you didn't request this, please ignore this email and your password will remain unchanged.</p>" +
                        "    <p>Best regards,<br>Your App Team</p>" +
                        "</div>", name, otp);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Error sending password reset email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
