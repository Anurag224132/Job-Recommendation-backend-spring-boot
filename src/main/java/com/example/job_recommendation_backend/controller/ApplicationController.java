package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;


    @PostMapping("/calculate-fit")
    public ResponseEntity<Long> calculateFitScore(@RequestBody CalculateFitScoreDto request) {
        return ResponseEntity.ok(applicationService.calculateFitScore(request));
    }


    @PostMapping
    public ResponseEntity<ApplicationResponseDto> createApplication(@RequestBody CreateApplicationRequestDto request) {
        return ResponseEntity.ok(applicationService.createApplication(request));
    }


    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<ApplicationResponseDto>> getRecruiterApplications(
            @PathVariable UUID recruiterId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(applicationService.allApplications(recruiterId, Role.recruiter, pageable));
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationResponseDto> updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequestDto request) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request));
    }


    @GetMapping("/user/{id}")
    public ResponseEntity<List<ApplicationResponseDto>> getUserApplications(
            @PathVariable UUID id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(applicationService.allApplications(id, Role.student, pageable));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getApplication(@PathVariable UUID id) {

        return ResponseEntity.ok(applicationService.checkApplication(null, id));
    }


    @PutMapping("/{appId}/schedule-interview")
    public ResponseEntity<ApplicationResponseDto> scheduleInterview(@PathVariable UUID appId, @RequestBody ScheduleInterviewRequestDto request,
                                                                    @RequestHeader("Authorization") String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "");
        UUID currentUserId = UserContext.get().getUserId();
        return ResponseEntity.ok(
                applicationService.scheduleInterview(
                        appId,
                        request.getInterviewDate(),
                        accessToken,
                        currentUserId
                )
        );
    }


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
        UUID userId = UserContext.get().getUserId();
        Role role = UserContext.get().getRole();
        return ResponseEntity.ok(applicationService.deleteApplication(id, role, userId));
    }
}