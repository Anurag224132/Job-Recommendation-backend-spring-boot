package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.ApplicationService;
import com.example.job_recommendation_backend.exception.CustomApiException;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }


    @PostMapping("/calculate-fit")
    public ResponseEntity<Map<String, Long>> calculateFitScore(@RequestBody CalculateFitScoreDto request) {
        long score = applicationService.calculateFitScore(request);
        return ResponseEntity.ok(Map.of("score", score));
    }

    @PostMapping("/calculate-fit-batch")
    public ResponseEntity<Map<UUID, Long>> calculateFitScores(@RequestBody CalculateFitScoreBatchRequestDto request) {
        return ResponseEntity.ok(applicationService.calculateFitScores(request));
    }


    @PostMapping
    public ResponseEntity<ApplicationResponseDto> createApplication(@Valid @RequestBody CreateApplicationRequestDto request) {
        return ResponseEntity.ok(applicationService.createApplication(request));
    }


    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<Page<ApplicationResponseDto>> getRecruiterApplications(
            @PathVariable UUID recruiterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = getPageable(page, size);
        var context = UserContext.get();
        UUID authenticatedId = context.getUserId();

        if (!authenticatedId.equals(recruiterId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "You can only view your own applications");
        }

        return ResponseEntity.ok(applicationService.allApplications(recruiterId, context.getRole(), pageable));
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationResponseDto> updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequestDto request) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request));
    }


    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<Page<ApplicationResponseDto>> getUserApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = getPageable(page, size);
        UUID userId = UserContext.get().getUserId();

        return ResponseEntity.ok(applicationService.allApplications(userId, Role.student, pageable));
    }


    @PreAuthorize("hasAnyRole('RECRUITER','STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.checkApplication(null, id));
    }


    @PutMapping("/{appId}/schedule-interview")
    public ApplicationResponseDto scheduleInterview(
            @PathVariable UUID appId,
            @Valid @RequestBody ScheduleInterviewRequestDto request) {

        return applicationService.scheduleInterview(appId, request.getInterviewDate());
    }


    @PreAuthorize("hasAnyRole('RECRUITER','STUDENT')")
    @GetMapping("/{applicationId}/download-resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable UUID applicationId) {
        Resource resource = applicationService.downloadResume(applicationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


    @GetMapping("/check")
    public ResponseEntity<ApplicationResponseDto> checkApplication(@RequestParam UUID jobId) {
        UUID userId = UserContext.get().getUserId();
        return ResponseEntity.ok(applicationService.checkApplication(userId, jobId));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteApplication(@PathVariable UUID id) {
        var context = UserContext.get();
        UUID userId = context.getUserId();
        Role role = context.getRole();
        return ResponseEntity.ok(applicationService.deleteApplication(id, role, userId));
    }

    private Pageable getPageable(int page, int size) {
        size = Math.min(size, 50);
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}