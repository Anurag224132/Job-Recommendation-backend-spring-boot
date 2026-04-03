package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.RecruiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/recruiter")
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    @Autowired
    private RecruiterService recruiterService;

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, List<JobAnalyticsDto>>> getAnalytics() {
        return ResponseEntity.ok(recruiterService.getAnalytics(getUserId() ));
    }

    @GetMapping("/skill-gap")
    public ResponseEntity<SkillGapDto> getGlobalSkillGap() {
        return ResponseEntity.ok(recruiterService.getGlobalSkillGap(getUserId()));
    }

    @GetMapping("/jobs/{jobId}/skill-gap")
    public ResponseEntity<SkillGapDto>skillGapAnalysis(@PathVariable UUID jobId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable= getPageable(page, size);
        return ResponseEntity.ok(recruiterService.skillGapAnalysis(jobId, getUserId(), pageable));
    }

    @GetMapping("/jobs/{jobId}/applicants")
    public ResponseEntity<List<ApplicantDto>> getJobApplicants(@PathVariable UUID jobId,@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = getPageable(page, size);
        return ResponseEntity.ok(recruiterService.getJobApplicants(jobId, getUserId(), pageable));
    }


    // Todo : have to change this return type to dto do not return entity directly
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<JobResponseDto> updateJob(@PathVariable UUID jobId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(recruiterService.updateJob(jobId, getUserId() , body));
    }

    @PatchMapping("/jobs/{jobId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleJobActive(@PathVariable UUID jobId) {
        return ResponseEntity.ok(recruiterService.toggleJobActive(jobId, getUserId() ));
    }

    @GetMapping("/applications/{appId}/download-resume")
    public ResponseEntity<InputStreamResource> downloadResume(@PathVariable UUID appId) {
        return recruiterService.downloadResume(appId, getUserId());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<List<RecruiterDashboardDto>> getDashboard(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = getPageable(page, size);
        return ResponseEntity.ok(
                recruiterService.getRecruiterDashboard(getUserId(), pageable)
        );
    }

    private UUID getUserId() {
        return UserContext.get().getUserId();
    }

    private Pageable getPageable(int page, int size) {
        size = Math.min(size, 50);
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}