package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.ApplicationStatus;
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
public class ApplicationResponseDto {
    private UUID id;
    private String studentName;
    private String jobTitle;
    private String companyName;
    private ApplicationStatus status;
    private String fitScore;
    private LocalDateTime createdAt;
    private String recruiterName;
    private JobResponseDto job;

    // Required for JPA constructor projection in ApplicationRepository
    public ApplicationResponseDto(UUID id, String studentName, String jobTitle, String companyName, ApplicationStatus status, String fitScore, LocalDateTime createdAt, String recruiterName) {
        this.id = id;
        this.studentName = studentName;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.status = status;
        this.fitScore = fitScore;
        this.createdAt = createdAt;
        this.recruiterName = recruiterName;
    }
}
