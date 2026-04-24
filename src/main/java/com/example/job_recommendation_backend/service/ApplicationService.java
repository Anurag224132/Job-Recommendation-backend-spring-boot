package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.enums.Role;
import java.util.Map;

import com.example.job_recommendation_backend.repository.projection.StudentAnalytics;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ApplicationService {

    long calculateFitScore(CalculateFitScoreDto req);
    Map<UUID, Long> calculateFitScores(CalculateFitScoreBatchRequestDto req);

    Resource downloadResume(UUID applicationId);

    Page<ApplicationResponseDto> allApplications(UUID userId, Role role, Pageable pageable);

    ApplicationResponseDto checkApplication(UUID userId, UUID jobId);

    String deleteApplication(UUID applicationId, Role role, UUID userId);

    StudentAnalytics getStudentAnalytics(UUID userId);

    ApplicationResponseDto updateApplicationStatus(UUID applicationId, UpdateStatusRequestDto request);

    ApplicationResponseDto createApplication(CreateApplicationRequestDto request);

    ApplicationResponseDto scheduleInterview(UUID appId, LocalDateTime interviewDate);
}
