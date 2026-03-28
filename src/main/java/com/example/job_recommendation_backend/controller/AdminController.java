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
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deleteUser(id));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<String> changeRole(@PathVariable UUID id, @RequestParam Role role) {
        return ResponseEntity.ok(adminService.changeRole(id, role));
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<JobResponseDto>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllJobs(pageable));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deleteJob(id));
    }

    @GetMapping("/metrics")
    public ResponseEntity<PlatformMetricsDto> getPlatformMetrics() {
        return ResponseEntity.ok(adminService.getPlatformMetrics());
    }

    @GetMapping("/analyticsCard")
    public ResponseEntity<AnalyticsCardDto> getAnalyticsCard(@RequestParam(defaultValue = "all") String range) {
        return ResponseEntity.ok(adminService.getAnalyticsCard(range));
    }

    @GetMapping("/users/{id}/analytics")
    public ResponseEntity<UserAnalyticsDto> getUserAnalytics(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getUserAnalytics(id));
    }

    @GetMapping("/users/search")
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.searchUsers(query, page, size));
    }

    @GetMapping("/jobs/search")
    public ResponseEntity<Page<JobResponseDto>> searchJobs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.searchJobs(query, page, size));
    }

    @PatchMapping("/jobs/{id}/toggle-status")
    public ResponseEntity<String> toggleJobStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.toggleJobStatus(id));
    }

    @GetMapping("/applications")
    public ResponseEntity<Page<ApplicationResponseDto>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllApplications(page, size));
    }

    @GetMapping("/interviews")
    public ResponseEntity<Page<InterviewResponseDto>> getAllInterviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllInterviews(page, size));
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<String> deleteApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deleteApplication(id));
    }

    @DeleteMapping("/interviews/{id}")
    public ResponseEntity<String> deleteInterview(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.deleteInterview(id));
    }
}

    