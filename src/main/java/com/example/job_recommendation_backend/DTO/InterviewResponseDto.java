package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.InterviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InterviewResponseDto {
    private UUID id;
    private String studentName;
    private String jobTitle;
    private InterviewStatus status;
    private String score;
    private LocalDateTime createdAt;
}
