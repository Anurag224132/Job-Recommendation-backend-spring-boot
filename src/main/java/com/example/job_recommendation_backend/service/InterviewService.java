package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.InterviewResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InterviewService {

    String deleteInterview(UUID id);

    Page<InterviewResponseDto> getAllInterviews(Pageable pageable);
}
