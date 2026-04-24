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

    String deleteJob(UUID id, UUID userId);

    PlatformMetricsDto getPlatformMetrics();

    AnalyticsCardDto getAnalyticsCard(String range);

    UserAnalyticsDto getUserAnalytics(UUID userId);

    Page<UserResponseDto> searchUsers(String query, Pageable pageable);

    Page<JobResponseDto> searchJobs(String query, Pageable pageable);

    String toggleJobStatus(UUID id);

    Page<ApplicationResponseDto> getAllApplications(UUID userId,Role role,Pageable pageable);

    Page<InterviewResponseDto> getAllInterviews(Pageable pageable);

    String deleteApplication(UUID applicationId, Role role,UUID userId);

    String deleteInterview(UUID id);

    JobResponseDto getJobDetails(UUID id);
}
