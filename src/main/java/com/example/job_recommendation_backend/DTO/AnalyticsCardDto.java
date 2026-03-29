package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsCardDto {
    private long totalUsers;
    private long activeJobs;
    private long totalApplications;
    private long totalCourses;
    private long studentCount;
    private long recruiterCount;
    private long adminCount;
    private List<UserActivityDto> userActivity;
}
