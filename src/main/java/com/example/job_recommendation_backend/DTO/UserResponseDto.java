package com.example.job_recommendation_backend.DTO;

import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private UUID id;
    private String name;
    private String email;
    private Role role;
    private java.util.List<String> skills;
    private String resumePath;
    private String profilePicture;
    private String bio;

    public static UserResponseDto fromEntity(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .skills(user.getSkills())
                .resumePath(user.getResumePath())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .build();
    }

    // Required for JPA constructor projection in UserRepository
    public UserResponseDto(UUID id, String name, String email, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }
}
