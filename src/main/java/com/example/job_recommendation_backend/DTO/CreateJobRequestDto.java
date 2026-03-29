package com.example.job_recommendation_backend.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateJobRequestDto {

    @NotNull
    private String title;
    @NotNull
    private String description;

    @NotNull
    private List<String> requiredSkills;
    private String location;
    private String salary;
    private String type;
    private String experience;
    private Boolean remote;

    @NotNull
    private String companyName;

}
