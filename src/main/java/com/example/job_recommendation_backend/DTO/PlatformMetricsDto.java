package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlatformMetricsDto {
    private long totalUsers;
    private long totalJobs;
    private long totalRecruiters;
    private long totalStudents;
}
