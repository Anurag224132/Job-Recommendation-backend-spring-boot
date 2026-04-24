package com.example.job_recommendation_backend.service;

import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.repository.projection.AnalyticsCounts;
import com.example.job_recommendation_backend.repository.projection.PlatformMetrics;
import com.example.job_recommendation_backend.repository.projection.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserService {

    User getUserById(UUID id);

    void updateUser(User user);

    Page<UserResponseDto> getAllUsers(Pageable pageable);

    String deleteUser(UUID id);

    Page<UserResponseDto> searchUsers(String query, Pageable pageable);

    AnalyticsCounts getAnalyticsCounts();

    List<UserActivity> getUserActivityDay(LocalDateTime startDate, LocalDateTime now);

    PlatformMetrics getPlatformMetrics();
}
