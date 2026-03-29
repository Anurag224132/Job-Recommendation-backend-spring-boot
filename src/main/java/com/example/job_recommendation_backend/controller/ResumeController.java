package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")   // ✅ same as Node
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    // ✅ POST /api/resumes (upload + parse)
    @PostMapping
    public ResponseEntity<?> uploadResume(
            @RequestParam("resume") MultipartFile file,   // same field name
            @RequestAttribute("userId") UUID userId       // from auth middleware equivalent
    ) {
        Map<String, Object> response = resumeService.uploadResume(file, userId);
        return ResponseEntity.ok(response);
    }

    // ✅ GET /api/resumes/download/:filename
    @GetMapping("/download/{filename}")
    public ResponseEntity<InputStreamResource> downloadResume(
            @PathVariable String filename
    ) {
        return resumeService.downloadResume(filename);
    }

    // ✅ POST /api/resumes/recommend-jobs
    @PostMapping("/recommend-jobs")
    public ResponseEntity<?> recommendJobs(@RequestBody Map<String, String> body) {

        UUID userId = UUID.fromString(body.get("userId"));
        Map<String, Object> response = resumeService.recommendJobs(userId);

        return ResponseEntity.ok(response);
    }
}