package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.ApplicationResponseDto;
import com.example.job_recommendation_backend.DTO.CreateJobRequestDto;
import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.repository.projection.RecruiterAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JobService {

    Page<JobResponseDto> getAllJobs(Pageable pageable);

    Job createJob(CreateJobRequestDto request, UUID userId);

    Page<JobResponseDto> getJobsByRecruiter(UUID userId, Pageable pageable);

    String deleteJob(UUID jobId, UUID userId);

    boolean toggleJobStatus(UUID jobId);

    List<ApplicationResponseDto> getUserApplications(UUID userId);

    JobResponseDto getJobById(UUID jobId);

    RecruiterAnalytics getRecruiterStats(UUID id);

    void deleteAllJobsByUserId(UUID id);

    Job findById(UUID id);

    Job updateJob(Job job);

    List<Job> findAllByIds(List<UUID> jobIds);

    Page<JobResponseDto> searchJobs(String q, String location, Boolean remote,
            String type, String experience, String skills,
            Pageable pageable);
}
