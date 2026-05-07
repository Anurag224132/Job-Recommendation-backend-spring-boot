package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    //Todo : remove all ResponseEntity<?> with some dto

    @PostMapping
    public ResponseEntity<?> uploadResume(@RequestParam("resume") MultipartFile file) {
        Map<String, Object> response = resumeService.uploadResume(file, getUserId());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadResume() {
        return resumeService.downloadResume(getUserId());
    }


    @PostMapping("/recommend-jobs")
    public ResponseEntity<?> recommendJobs() {

        Map<String, Object> response = resumeService.recommendJobs(getUserId());

        return ResponseEntity.ok(response);
    }

    private UUID getUserId(){
        return UserContext.get().getUserId();
    }
}