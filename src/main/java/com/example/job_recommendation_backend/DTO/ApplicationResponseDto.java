package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponseDto {
    private UUID id;
    private String studentName;
    private String jobTitle;
    private String companyName;
    private ApplicationStatus status;
    private String fitScore;
    private LocalDateTime createdAt;
}
