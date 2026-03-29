package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.service.RecruiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/recruiter")   // base path
public class RecruiterController {

    @Autowired
    private RecruiterService recruiterService;

    // =========================
    // ✅ GET /analytics
    // =========================
    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(recruiterService.getAnalytics(userId));
    }

    // =========================
    // ✅ GET /recruiter/analytics
    // =========================
    @GetMapping("/recruiter/analytics")
    public ResponseEntity<?> getRecruiterAnalytics(
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(recruiterService.getRecruiterAnalytics(userId));
    }

    // =========================
    // ✅ GET /jobs/:jobId/skill-gap
    // =========================
    @GetMapping("/jobs/{jobId}/skill-gap")
    public ResponseEntity<?> skillGapAnalysis(
            @PathVariable UUID jobId,
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(recruiterService.skillGapAnalysis(jobId, userId));
    }

    // =========================
    // ✅ GET /jobs/:jobId/applicants
    // =========================
    @GetMapping("/jobs/{jobId}/applicants")
    public ResponseEntity<?> getJobApplicants(
            @PathVariable UUID jobId,
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(recruiterService.getJobApplicants(jobId, userId));
    }

    // =========================
    // ✅ PUT /jobs/:jobId
    // =========================
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<?> updateJob(
            @PathVariable UUID jobId,
            @RequestBody Map<String, Object> body,
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(recruiterService.updateJob(jobId, userId, body));
    }

    // =========================
    // ✅ PATCH /jobs/:jobId/toggle
    // =========================
    @PatchMapping("/jobs/{jobId}/toggle")
    public ResponseEntity<?> toggleJobActive(
            @PathVariable UUID jobId,
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(recruiterService.toggleJobActive(jobId, userId));
    }

    // =========================
    // ✅ GET /applications/:appId/download-resume
    // =========================
    @GetMapping("/applications/{appId}/download-resume")
    public ResponseEntity<InputStreamResource> downloadResume(
            @PathVariable UUID appId
    ) {
        return recruiterService.downloadResume(appId);
    }

    // =========================
    // ✅ GET /recruiter/:recruiterId
    // =========================
    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<?> getRecruiterDashboard(
            @PathVariable UUID recruiterId,
            @RequestAttribute("userId") UUID userId
    ) {
        return ResponseEntity.ok(
                recruiterService.getRecruiterDashboard(recruiterId, userId)
        );
    }
}