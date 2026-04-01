package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.DTO.CreateJobRequestDto;
import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.JobService;
import jakarta.validation.Valid;
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

        Pageable pageable = getPageable(page, size);
        
        return ResponseEntity.ok(jobService.searchJobs(q, location, remote, type, experience, skills, pageable));
    }

    // Todo : do not return job directly make dto
    @PreAuthorize("hasRole('RECRUITER')")
    @PostMapping
    public ResponseEntity<Job> createJob(@Valid @RequestBody CreateJobRequestDto request) {
        return ResponseEntity.ok(jobService.createJob(request, getUserId()));
    }


    // Todo : do not return job directly make dto
    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/my-jobs")
    public ResponseEntity<Page<Job>> getMyJobs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = getPageable(page, size);
        return ResponseEntity.ok(jobService.getJobsByRecruiter(getUserId(),pageable));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        jobService.deleteJob(id, getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    private Pageable getPageable(int page, int size) {
        size = Math.min(size, 50);
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }

    private UUID getUserId() {
        return UserContext.get().getUserId();
    }
}
