package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateStatusRequestDto {
    @NotNull
    private ApplicationStatus status;
    private String notes;
}
