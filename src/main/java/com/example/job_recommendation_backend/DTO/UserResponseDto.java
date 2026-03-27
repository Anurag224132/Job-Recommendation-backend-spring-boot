package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponseDto {
    private UUID id;
    private String name;
    private String email;
    private Role role;

    public static UserResponseDto fromEntity(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
