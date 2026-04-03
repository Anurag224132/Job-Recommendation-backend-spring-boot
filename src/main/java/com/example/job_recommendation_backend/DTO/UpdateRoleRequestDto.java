package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateRoleRequestDto {
    private Role role;
}
