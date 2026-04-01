package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class JobAnalyticsDto {
    private UUID jobId;
    private String title;
    private Long applicationCount;
}