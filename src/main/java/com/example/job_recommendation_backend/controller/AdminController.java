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

    @GetMapping("/users/{userId}/analytics")
    public ResponseEntity<UserAnalyticsDto> getUserAnalytics(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getUserAnalytics(userId));
    }

    @GetMapping("/jobs/{jobId}/details")
    public ResponseEntity<JobDetailsDto> getJobDetails(@PathVariable UUID jobId) {
        return ResponseEntity.ok(adminService.getJobDetails(jobId));
    }
}
