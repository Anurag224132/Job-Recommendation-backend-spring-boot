package com.example.job_recommendation_backend.service.Impl;

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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

            String body = String.format(
                    "<p>Hello %s,</p>" +
                            "<p>Please use the OTP below to verify your email:</p>" +
                            "<div style=\"background: #f4f4f4; padding: 10px; text-align: center; font-size: 24px; letter-spacing: 5px;\">" +
                            "%s" +
                            "</div>" +
                            "<p>This OTP is valid for 15 minutes.</p>",
                    name, otp
            );

            String htmlContent = buildEmailTemplate("Email Verification", body);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Error sending verification email to {}: {}", email, e.getMessage());
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

            String body = String.format(
                    "<p>Hello %s,</p>" +
                            "<p>Use the OTP below to reset your password:</p>" +
                            "<div style=\"background: #f4f4f4; padding: 10px; text-align: center; font-size: 24px; letter-spacing: 5px;\">" +
                            "%s" +
                            "</div>" +
                            "<p>This OTP is valid for 15 minutes.</p>",
                    name, otp
            );
            String htmlContent = buildEmailTemplate("Password Reset", body);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        }catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendInterviewEmail(String email, String name, String jobTitle, LocalDateTime date, String link) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Interview Scheduled - " + jobTitle);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            String formattedDate = date.format(formatter);

            String body = String.format(
                    "<p>Hello %s,</p>" +
                            "<p>Your interview for <strong>%s</strong> has been scheduled.</p>" +
                            "<p><strong>Date & Time:</strong> %s</p>" +
                            "<p style=\"margin-top:20px;\">" +
                            "<a href=\"%s\" target=\"_blank\" " +
                            "style=\"background-color:#2563eb;color:white;padding:12px 18px;text-decoration:none;border-radius:6px;display:inline-block;font-weight:bold;\">" +
                            "Join Interview" +
                            "</a>" +
                            "</p>" +
                            "<p style=\"margin-top:20px; color:#6b7280; font-size:14px;\">" +
                            "Please join 5 minutes before the scheduled time." +
                            "</p>",
                    name, jobTitle, formattedDate, link
            );

            String htmlContent = buildEmailTemplate("Interview Scheduled 🎉", body);

            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Interview email sent to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send interview email to {}: {}", email, e.getMessage());
        }
    }

    private String buildEmailTemplate(String title, String bodyContent) {
        return String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                        "<h2 style=\"color: #2563eb;\">%s</h2>" +
                        "%s" +
                        "<hr style=\"margin: 20px 0; border: none; border-top: 1px solid #eee;\"/>" +
                        "<p style=\"color: #6b7280; font-size: 14px;\">" +
                        "If you didn't request this, you can safely ignore this email." +
                        "</p>" +
                        "<p style=\"margin-top: 20px;\">" +
                        "<strong>Best regards,</strong><br/>" +
                        "Your App Team" +
                        "</p>" +
                        "</div>",
                title,
                bodyContent
        );
    }
}
