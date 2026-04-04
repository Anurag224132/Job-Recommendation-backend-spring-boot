package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.from-email:}")
    private String fromEmail;

    @Value("${brevo.from-name:Job Recommendation Platform}")
    private String fromName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private void sendEmailViaBrevo(String toEmail, String toName, String subject, String htmlContent) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", toEmail, "name", toName)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent to {} successfully via Brevo. Response: {}", toEmail, response.body());
            } else {
                log.error("Failed to send email to {} via Brevo. Status: {}, Response: {}", toEmail, response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("Exception occurred while sending email to {} via Brevo: {}", toEmail, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendVerificationEmail(String email, String name, String otp) {
        String title = "Verify Your Email Address";
        String body = String.format(
                "<p>Hello %s,</p>" +
                        "<p>Please use the OTP below to verify your email:</p>" +
                        "<div style=\"background: #f4f4f4; padding: 10px; text-align: center; font-size: 24px; letter-spacing: 5px;\">" +
                        "%s" +
                        "</div>" +
                        "<p>This OTP is valid for 15 minutes.</p>",
                name, otp
        );

        String htmlContent = buildEmailTemplate(title, body);
        sendEmailViaBrevo(email, name, title, htmlContent);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String email, String name, String otp) {
        String title = "Password Reset OTP";
        String body = String.format(
                "<p>Hello %s,</p>" +
                        "<p>Use the OTP below to reset your password:</p>" +
                        "<div style=\"background: #f4f4f4; padding: 10px; text-align: center; font-size: 24px; letter-spacing: 5px;\">" +
                        "%s" +
                        "</div>" +
                        "<p>This OTP is valid for 15 minutes.</p>",
                name, otp
        );
        String htmlContent = buildEmailTemplate(title, body);
        sendEmailViaBrevo(email, name, title, htmlContent);
    }

    @Async
    @Override
    public void sendInterviewEmail(String email, String name, String jobTitle, LocalDateTime date, String link) {
        String title = "Interview Scheduled - " + jobTitle;
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
        sendEmailViaBrevo(email, name, title, htmlContent);
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
