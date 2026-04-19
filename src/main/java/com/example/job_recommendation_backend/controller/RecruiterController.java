package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.service.RecruiterService;
import com.example.job_recommendation_backend.utility.AuthUtil;
import com.example.job_recommendation_backend.utility.PaginationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    private PaginationUtil paginationUtil;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, List<JobAnalyticsDto>>> getAnalytics() {
        return ResponseEntity.ok(recruiterService.getAnalytics(authUtil.getCurrentUserId() ));
    }

    @GetMapping("/skill-gap")
    public ResponseEntity<SkillGapDto> getGlobalSkillGap() {
        return ResponseEntity.ok(recruiterService.getGlobalSkillGap(authUtil.getCurrentUserId()));
    }

    @GetMapping("/jobs/{jobId}/skill-gap")
    public ResponseEntity<SkillGapDto>skillGapAnalysis(@PathVariable UUID jobId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = paginationUtil.getPageable(page, size);
        return ResponseEntity.ok(recruiterService.skillGapAnalysis(jobId, authUtil.getCurrentUserId(), pageable));
    }

    @GetMapping("/jobs/{jobId}/applicants")
    public ResponseEntity<List<ApplicantDto>> getJobApplicants(@PathVariable UUID jobId,@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = paginationUtil.getPageable(page, size);
        return ResponseEntity.ok(recruiterService.getJobApplicants(jobId, authUtil.getCurrentUserId(), pageable));
    }


    // Todo : have to change this return type to dto do not return entity directly
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<JobResponseDto> updateJob(@PathVariable UUID jobId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(recruiterService.updateJob(jobId, authUtil.getCurrentUserId() , body));
    }

    @PatchMapping("/jobs/{jobId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleJobActive(@PathVariable UUID jobId) {
        return ResponseEntity.ok(recruiterService.toggleJobActive(jobId, authUtil.getCurrentUserId() ));
    }

    @GetMapping("/applications/{appId}/download-resume")
    public ResponseEntity<InputStreamResource> downloadResume(@PathVariable UUID appId) {
        return recruiterService.downloadResume(appId, authUtil.getCurrentUserId());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<List<RecruiterDashboardDto>> getDashboard(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = paginationUtil.getPageable(page, size);
        return ResponseEntity.ok(
                recruiterService.getRecruiterDashboard(authUtil.getCurrentUserId(), pageable)
        );
    }

}