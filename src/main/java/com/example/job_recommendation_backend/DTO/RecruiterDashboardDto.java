package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class RecruiterDashboardDto {
    private UUID jobId;
    private String title;
    private List<ApplicantDto> applications;
}