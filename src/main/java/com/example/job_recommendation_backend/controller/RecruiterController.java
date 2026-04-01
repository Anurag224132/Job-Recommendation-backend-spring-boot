package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.service.RecruiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {

    @Autowired
    private RecruiterService recruiterService;


    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(recruiterService.getAnalytics(userId));
    }


    @GetMapping("/recruiter/analytics")
    public ResponseEntity<?> getRecruiterAnalytics(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(recruiterService.getRecruiterAnalytics(userId));
    }


    @GetMapping("/jobs/{jobId}/skill-gap")
    public ResponseEntity<?> skillGapAnalysis(@PathVariable UUID jobId, @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(recruiterService.skillGapAnalysis(jobId, userId));
    }


    @GetMapping("/jobs/{jobId}/applicants")
    public ResponseEntity<?> getJobApplicants(@PathVariable UUID jobId, @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(recruiterService.getJobApplicants(jobId, userId));
    }


    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<?> updateJob(@PathVariable UUID jobId, @RequestBody Map<String, Object> body, @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(recruiterService.updateJob(jobId, userId, body));
    }


    @PatchMapping("/jobs/{jobId}/toggle")
    public ResponseEntity<?> toggleJobActive(@PathVariable UUID jobId, @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(recruiterService.toggleJobActive(jobId, userId));
    }


    @GetMapping("/applications/{appId}/download-resume")
    public ResponseEntity<InputStreamResource> downloadResume(@PathVariable UUID appId) {
        return recruiterService.downloadResume(appId);
    }


    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<?> getRecruiterDashboard(@PathVariable UUID recruiterId, @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(
                recruiterService.getRecruiterDashboard(recruiterId, userId)
        );
    }
}