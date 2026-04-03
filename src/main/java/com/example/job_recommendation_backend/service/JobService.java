package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.ApplicationResponseDto;
import com.example.job_recommendation_backend.DTO.CreateJobRequestDto;
import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JobService {

    Page<Job> getAllActiveJobs(Pageable pageable);

    Job createJob(CreateJobRequestDto request, UUID userId);

    Page<JobResponseDto> getJobsByRecruiter(UUID userId,Pageable pageable);

    void deleteJob(UUID jobId, UUID userId);

    List<ApplicationResponseDto> getUserApplications(UUID userId);

    JobResponseDto getJobById(UUID jobId);
    
    Page<JobResponseDto> searchJobs(String q, String location, Boolean remote, 
                                   String type, String experience, String skills, 
                                   Pageable pageable);
}
