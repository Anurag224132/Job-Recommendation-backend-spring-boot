package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ApplicantDto {
    private UUID applicationId;
    private String name;
    private String email;
    private List<String> skills;
    private String jobTitle;
}