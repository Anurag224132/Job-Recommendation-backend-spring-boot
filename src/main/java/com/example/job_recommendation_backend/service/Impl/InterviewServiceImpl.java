package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.InterviewResponseDto;
import com.example.job_recommendation_backend.entity.Interview;
import com.example.job_recommendation_backend.exception.ResourceNotFoundException;
import com.example.job_recommendation_backend.repository.InterviewRepository;
import com.example.job_recommendation_backend.service.InterviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InterviewServiceImpl implements InterviewService {

    @Autowired
    private InterviewRepository interviewRepository;

    @Override
    public String deleteInterview(UUID id) {
        Interview interview = interviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id.toString()));
        interview.setDeletedAt(LocalDateTime.now());
        interviewRepository.save(interview);
        return "Interview deleted successfully";
    }

    @Override
    public Page<InterviewResponseDto> getAllInterviews(Pageable pageable) {
        return interviewRepository.findAllInterviews(pageable);
    }
}
