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
@RequestMapping("/api/resumes")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;


    @PostMapping
    public ResponseEntity<?> uploadResume(@RequestParam("resume") MultipartFile file, @RequestAttribute("userId") UUID userId) {
        Map<String, Object> response = resumeService.uploadResume(file, userId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/download/{filename}")
    public ResponseEntity<InputStreamResource> downloadResume(@PathVariable String filename) {
        return resumeService.downloadResume(filename);
    }


    @PostMapping("/recommend-jobs")
    public ResponseEntity<?> recommendJobs(@RequestBody Map<String, String> body) {

        UUID userId = UUID.fromString(body.get("userId"));
        Map<String, Object> response = resumeService.recommendJobs(userId);

        return ResponseEntity.ok(response);
    }
}