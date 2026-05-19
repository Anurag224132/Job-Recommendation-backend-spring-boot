package com.example.job_recommendation_backend.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

public interface ResumeService {

    Map<String, Object> uploadResume(MultipartFile file, UUID userId);

    ResponseEntity<?> downloadResume(UUID userID);

    Map<String, Object> recommendJobs(UUID userId);

    /** Returns the resume URL (Cloudinary or local path) for the authenticated user */
    Map<String, String> getResumeUrl(UUID userId);
}