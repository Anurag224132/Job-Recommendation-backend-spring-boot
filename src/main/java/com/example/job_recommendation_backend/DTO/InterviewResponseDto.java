package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponseDto {
    private UUID id;
    private String studentName;
    private String jobTitle;
    private InterviewStatus status;
    private String score;
    private LocalDateTime createdAt;
}
