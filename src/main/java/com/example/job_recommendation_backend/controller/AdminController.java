package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponseDto> deleteUser(@PathVariable UUID id) {
        String message = adminService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponseDto(message, true));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponseDto> changeRole(@PathVariable UUID id, @RequestParam Role role) {
        String message = adminService.changeRole(id, role);
        return ResponseEntity.ok(new ApiResponseDto(message, true));
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<JobResponseDto>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllJobs(pageable));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<ApiResponseDto> deleteJob(@PathVariable UUID id) {
        String message = adminService.deleteJob(id);
        return ResponseEntity.ok(new ApiResponseDto(message, true));
    }

    @GetMapping("/metrics")
    public ResponseEntity<PlatformMetricsDto> getPlatformMetrics() {
        return ResponseEntity.ok(adminService.getPlatformMetrics());
    }

    @GetMapping("/analytics/card")
    public ResponseEntity<AnalyticsCardDto> getAnalyticsCard(@RequestParam(defaultValue = "all") String range) {
        return ResponseEntity.ok(adminService.getAnalyticsCard(range));
    }

    @GetMapping("/users/{id}/analytics")
    public ResponseEntity<UserAnalyticsDto> getUserAnalytics(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUserAnalytics(id));
    }

    @GetMapping("/users/search-by-query")
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.searchUsers(query, page, size));
    }

    @GetMapping("/jobs/search-by-query")
    public ResponseEntity<Page<JobResponseDto>> searchJobs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.searchJobs(query, page, size));
    }

    @PatchMapping("/jobs/{id}/toggle-status")
    public ResponseEntity<ApiResponseDto> toggleJobStatus(@PathVariable UUID id) {
        String message = adminService.toggleJobStatus(id);
        return ResponseEntity.ok(new ApiResponseDto(message, true));
    }

    @GetMapping("/applications")
    public ResponseEntity<Page<ApplicationResponseDto>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllApplications(pageable));
    }

    @GetMapping("/interviews")
    public ResponseEntity<Page<InterviewResponseDto>> getAllInterviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllInterviews(pageable));
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<ApiResponseDto> deleteApplication(@PathVariable UUID id) {
        String message = adminService.deleteApplication(id);
        return ResponseEntity.ok(new ApiResponseDto(message, true));
    }

    @DeleteMapping("/interviews/{id}")
    public ResponseEntity<ApiResponseDto> deleteInterview(@PathVariable UUID id) {
        String message = adminService.deleteInterview(id);
        return ResponseEntity.ok(new ApiResponseDto(message, true));
    }
}

    