package com.example.job_recommendation_backend.DTO;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponseDto {
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
    private LocalDateTime updatedAt;
    private RecruiterDto recruiter;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruiterDto {
        private String name;
        private String email;
    }
}
