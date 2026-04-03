package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalculateFitScoreBatchRequestDto {
    private List<UUID> jobIds;
    private List<String> resumeSkills;
}
