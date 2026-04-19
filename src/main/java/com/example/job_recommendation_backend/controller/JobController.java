package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.CreateJobRequestDto;
import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.service.JobService;
import com.example.job_recommendation_backend.utility.AuthUtil;
import com.example.job_recommendation_backend.utility.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Autowired
    private PaginationUtil paginationUtil;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping("/search")
    public ResponseEntity<Page<JobResponseDto>> searchJobs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String skills,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = paginationUtil.getPageable(page, size);

        return ResponseEntity.ok(jobService.searchJobs(q, location, remote, type, experience, skills, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<Job>> getJobs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = paginationUtil.getPageable(page, size);
        return ResponseEntity.ok(jobService.getAllActiveJobs(pageable));
    }

    // Todo : do not return job directly make dto
    @PreAuthorize("hasRole('RECRUITER')")
    @PostMapping
    public ResponseEntity<Job> createJob(@Valid @RequestBody CreateJobRequestDto request) {
        return ResponseEntity.ok(jobService.createJob(request, authUtil.getCurrentUserId()));
    }


    // Todo : do not return job directly make dto
    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/my-jobs")
    public ResponseEntity<Page<JobResponseDto>> getMyJobs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = paginationUtil.getPageable(page, size);
        return ResponseEntity.ok(jobService.getJobsByRecruiter(authUtil.getCurrentUserId(),pageable));
    }

    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        jobService.deleteJob(id, authUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

}
