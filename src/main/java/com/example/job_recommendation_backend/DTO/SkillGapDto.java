package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SkillGapDto {
    private List<String> missingSkills;
}