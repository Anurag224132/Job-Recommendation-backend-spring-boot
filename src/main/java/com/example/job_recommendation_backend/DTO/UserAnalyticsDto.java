package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAnalyticsDto {
    private UUID userId;
    private String name;
    private String email;
    private Role role;
    private LocalDateTime lastActive;
    private LocalDateTime createdAt;
    private List<String> skills;
    private int profileCompleted;

    // Student specific
    private Long coursesEnrolled;
    private Long jobsApplied;
    private Long jobsRejected;
    private Long jobsInterviewed;
    private Integer rejectionRate;
    private Integer interviewRate;

    // Recruiter specific
    private Integer jobsPosted;
    private Long totalApplications;
    private Long rejected;
    private Long interviewsScheduled;

    // Common role-independent activities placeholder
    private List<Object> recentActivities;
}
