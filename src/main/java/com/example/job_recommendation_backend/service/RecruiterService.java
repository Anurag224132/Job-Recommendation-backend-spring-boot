package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.entity.Job;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RecruiterService {

    Map<String, List<JobAnalyticsDto>> getAnalytics(UUID userId);

    SkillGapDto skillGapAnalysis(UUID jobId, UUID userId, Pageable pageable);

    List<ApplicantDto> getJobApplicants(UUID jobId, UUID userId, Pageable pageable);

    Job updateJob(UUID jobId, UUID userId, Map<String, Object> body);

    Map<String, Object> toggleJobActive(UUID jobId, UUID userId);

    ResponseEntity<InputStreamResource> downloadResume(UUID appId,UUID userId);

    List<RecruiterDashboardDto> getRecruiterDashboard(UUID userId, Pageable pageable);
}