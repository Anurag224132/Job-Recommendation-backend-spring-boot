package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.CreateJobRequestDto;
import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

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

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        return ResponseEntity.ok(jobService.searchJobs(q, location, remote, type, experience, skills, pageable));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody CreateJobRequestDto request) {
        var context = UserContext.get();
        return ResponseEntity.ok(jobService.createJob(request, context.getUserId(), context.getRole()));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getMyJobs() {
        var context = UserContext.get();
        return ResponseEntity.ok(jobService.getJobsByRecruiter(context.getUserId(), context.getRole()));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        var context = UserContext.get();
        jobService.deleteJob(id, context.getUserId(), context.getRole());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }
}
