package com.example.job_recommendation_backend.service.Impl;

import com.cloudinary.Cloudinary;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.ResumeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import com.example.job_recommendation_backend.exception.CustomApiException;
import com.example.job_recommendation_backend.exception.ResourceNotFoundException;
import org.springframework.util.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.net.URI;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final RestTemplate restTemplate;
    private final Cloudinary cloudinary;

    @Value("${ml.api.url}")
    private String mlApiUrl;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/resumes";

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    @Override
    public Map<String, Object> uploadResume(MultipartFile file, UUID userId) {
        try {
            validateFile(file);

            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
            }

            final User finalUser = user;
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            // Task 1: Send to ML API
            CompletableFuture<Map<String, Object>> mlTask = CompletableFuture.supplyAsync(() -> {
                Map<String, Object> mlData = new HashMap<>();
                if (mlApiUrl != null && !mlApiUrl.isEmpty()) {
                    try {
                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("resume", new ByteArrayResource(fileBytes) {
                            @Override
                            public String getFilename() {
                                return originalFilename;
                            }
                        });

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                mlApiUrl + "/parse_resume",
                                request,
                                Map.class);
                        mlData = response.getBody() != null ? response.getBody() : new HashMap<>();
                        log.info("📄 ML Resume Parse Result: {}", mlData);
                    } catch (Exception e) {
                        log.warn("⚠️ ML Resume Parsing failed: {}", e.getMessage());
                    }
                }
                return mlData;
            });

            CompletableFuture<Map<String, String>> cloudinaryTask = CompletableFuture.supplyAsync(() -> {
                Map<String, String> cloudData = new HashMap<>();
                if (finalUser != null) {
                    // ✅ DO NOT delete the old Cloudinary resume.
                    // Old applications hold a snapshot URL pointing to the previous resume.
                    // Deleting it would break recruiter access to that application's resume.
                    // If the old path was a local file (pre-Cloudinary), clean it up.
                    if (finalUser.getResumePath() != null && !finalUser.getResumePath().startsWith("http")) {
                        try {
                            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                            Path oldPath = uploadPath.resolve(finalUser.getResumePath());
                            Files.deleteIfExists(oldPath);
                            log.info("🗑️ Cleaned up old local resume file");
                        } catch (Exception e) {
                            log.warn("⚠️ Failed to delete old local resume file");
                        }
                    }

                    // Upload new resume with a unique public_id (userId + timestamp)
                    // so each version is stored separately and application snapshots remain valid.
                    try {
                        String uniquePublicId = "skillspark_resumes/" + finalUser.getId() + "_" + System.currentTimeMillis();
                        Map<String, Object> uploadOptions = new HashMap<>();
                        uploadOptions.put("public_id", uniquePublicId);
                        uploadOptions.put("resource_type", "raw"); // raw = correct type for PDFs/docs
                        uploadOptions.put("type", "upload");        // publicly accessible
                        uploadOptions.put("access_mode", "public");
                        uploadOptions.put("overwrite", false);      // never overwrite — always new file
                        Map uploadResult = cloudinary.uploader().upload(fileBytes, uploadOptions);
                        cloudData.put("secure_url", (String) uploadResult.get("secure_url"));
                        cloudData.put("public_id", (String) uploadResult.get("public_id"));
                        log.info("☁️ Resume uploaded to Cloudinary: {}", cloudData.get("public_id"));
                    } catch (Exception e) {
                        throw new RuntimeException("Cloudinary upload failed", e);
                    }
                }
                return cloudData;
            });

            // Wait for both concurrent tasks
            CompletableFuture.allOf(mlTask, cloudinaryTask).join();

            Map<String, Object> mlData = mlTask.get();
            Map<String, String> cloudData = cloudinaryTask.get();

            // Finalize and save DB
            if (finalUser != null && cloudData.containsKey("secure_url")) {
                finalUser.setResumePath(cloudData.get("secure_url"));
                finalUser.setResumePublicId(cloudData.get("public_id"));

                Object skillsObj = mlData.get("skills");
                if (skillsObj instanceof List<?>) {
                    List<String> skills = ((List<?>) skillsObj).stream()
                            .map(Object::toString)
                            .map(s -> s.toLowerCase().trim())
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    log.info("✅ Extracted {} skills for user {}: {}", skills.size(), finalUser.getEmail(), skills);
                    finalUser.setSkills(skills);
                }

                userRepository.save(finalUser);
                log.info("💾 User profile updated successfully with new Cloudinary resume and skills");
            }

            return mlData;

        } catch (Exception err) {
            throw handleError(err, "resume upload");
        }
    }

    @Override
    public Map<String, String> getResumeUrl(UUID userId) {
        UUID currentUserId = UserContext.get().getUserId();
        if (!currentUserId.equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        String path = user.getResumePath();
        if (path == null || path.isBlank()) {
            throw new CustomApiException(HttpStatus.NOT_FOUND, "No resume uploaded yet. Please upload your resume first.");
        }
        return Map.of(
            "url", path,
            "isCloud", String.valueOf(path.startsWith("http"))
        );
    }

    @Override
    public ResponseEntity<?> downloadResume(UUID userId) {
        try {
            UUID currentUserId = UserContext.get().getUserId();

            if (!currentUserId.equals(userId)) {
                throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

            String filename = user.getResumePath();

            if (filename == null || filename.isBlank()) {
                throw new CustomApiException(HttpStatus.NOT_FOUND, "No resume uploaded yet");
            }

            Resource resource;
            String downloadName;

            if (filename.startsWith("http")) {
                // Cloudinary URL — proxy download through backend
                RestTemplate rt = new RestTemplate();
                byte[] fileBytes = rt.getForObject(filename, byte[].class);
                if (fileBytes == null) {
                    throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download from Cloudinary");
                }
                downloadName = "resume_" + user.getName().replaceAll("\\s+", "_") + ".pdf";
                resource = new ByteArrayResource(fileBytes);
            } else {
                // Local file fallback
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Path filePath = uploadPath.resolve(filename).normalize();

                if (!filePath.startsWith(uploadPath)) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
                }
                if (!Files.exists(filePath)) {
                    throw new CustomApiException(HttpStatus.NOT_FOUND, "Resume file not found");
                }
                resource = new UrlResource(filePath.toUri());
                downloadName = filename;
            }

            // Use inline so iframe/viewer can render the PDF directly;
            // the frontend Download button uses the blob URL to force a save.
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(resource.getInputStream()));

        } catch (CustomApiException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("❌ Error downloading resume", e);
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading resume");
        }
    }


    @Override
    public Map<String, Object> recommendJobs(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

            if (user.getSkills() == null || user.getSkills().isEmpty()) {
                throw new CustomApiException(HttpStatus.NOT_FOUND, "User skills not found");
            }

            List<Job> jobs = jobRepository.findAll();

            List<Map<String, Object>> jobList = jobs.stream().map(job -> {
                Map<String, Object> map = new HashMap<>();
                map.put("_id", job.getId().toString());
                map.put("title", job.getTitle());
                map.put("requiredSkills", job.getRequiredSkills());
                return map;
            }).toList();

            Map<String, Object> payload = new HashMap<>();
            payload.put("skills", user.getSkills());
            payload.put("jobs", jobList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            if (mlApiUrl == null || mlApiUrl.isEmpty()) {
                Map<String, Object> fallbackData = new HashMap<>();
                fallbackData.put("matches", new ArrayList<>());
                return fallbackData;
            }

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        mlApiUrl + "/match_jobs",
                        request,
                        Map.class);
                return response.getBody();
            } catch (Exception err) {
                Map<String, Object> fallbackData = new HashMap<>();
                fallbackData.put("matches", new ArrayList<>());
                return fallbackData;
            }
        } catch (Exception err) {
            throw handleError(err, "job recommendation");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "No file uploaded");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new CustomApiException(HttpStatus.valueOf(413), "File too large");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        String filename = originalName.toLowerCase();
        String ext = filename.substring(filename.lastIndexOf("."));
        List<String> allowed = List.of(".pdf", ".doc", ".docx", ".txt");
        if (!allowed.contains(ext)) {
            throw new CustomApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF, DOC, DOCX and TXT files are allowed");
        }
    }

    private CustomApiException handleError(Exception err, String context) {
        System.err.println("❌ " + context + " error: " + err.getMessage());
        if (err instanceof org.springframework.web.client.HttpStatusCodeException ex) {
            return new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Request failed (API error): " + ex.getResponseBodyAsString());
        } else if (err instanceof org.springframework.web.client.ResourceAccessException) {
            return new CustomApiException(HttpStatus.SERVICE_UNAVAILABLE, "No response from service");
        } else {
            return new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, context + " failed: " + err.getMessage());
        }
    }
}