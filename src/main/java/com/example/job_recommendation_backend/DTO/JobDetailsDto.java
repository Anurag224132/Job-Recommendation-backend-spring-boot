package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailsDto {
    private UUID id;
    private String title;
    private String description;
    private List<String> requiredSkills;
    private String location;
    private String salary;
    private String type;
    private String experience;
    private Boolean remote;
    private Boolean isActive;
    private String companyName;
    private LocalDateTime createdAt;
    private JobResponseDto.RecruiterDto recruiter;

    private long applications;
    private int averageMatchScore;
    private List<TopApplicantDto> topApplicants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopApplicantDto {
        private String name;
        private String email;
        private int matchScore;
        private ApplicationStatus status;
    }
}
