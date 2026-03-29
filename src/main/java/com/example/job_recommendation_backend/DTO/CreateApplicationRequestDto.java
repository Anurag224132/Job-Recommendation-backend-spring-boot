package com.example.job_recommendation_backend.DTO;

import jakarta.validation.constraints.NotNull;
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
public class CreateApplicationRequestDto {

    @NotNull
    private UUID jobId;

    @NotNull
    private UUID userId;

    @NotNull
    private List<String> resumeSkills;

}
