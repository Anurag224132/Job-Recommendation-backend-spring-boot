package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface AdminService {
    Page<UserResponseDto> getAllUsers(Pageable pageable);

    String deleteUser(UUID id);

    String changeRole(UUID id, Role role);

    Page<JobResponseDto> getAllJobs(Pageable pageable);

    String deleteJob(UUID id);

    PlatformMetricsDto getPlatformMetrics();

    AnalyticsCardDto getAnalyticsCard(String range);

    UserAnalyticsDto getUserAnalytics(UUID userId);

    Page<UserResponseDto> searchUsers(String query, int page, int size);

    Page<JobResponseDto> searchJobs(String query, int page, int size);

    String toggleJobStatus(UUID id);

    Page<ApplicationResponseDto> getAllApplications(Pageable pageable);

    Page<InterviewResponseDto> getAllInterviews(Pageable pageable);

    String deleteApplication(UUID id);

    String deleteInterview(UUID id);
}
