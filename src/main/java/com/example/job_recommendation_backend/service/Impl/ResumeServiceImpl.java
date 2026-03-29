package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.service.ResumeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeServiceImpl implements ResumeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ml.api.url}")
    private String mlApiUrl;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/resumes";

    // =========================
    // ✅ Create Upload Directory
    // =========================
    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    // =========================
    // ✅ Upload + Parse Resume
    // =========================
    @Override
    public Map<String, Object> uploadResume(MultipartFile file, UUID userId) {

        try {
            validateFile(file);

            // sanitize filename
            String originalName = file.getOriginalFilename();
            String sanitized = originalName.replaceAll("[^a-zA-Z0-9_.-]", "_");
            String filename = System.currentTimeMillis() + "-" + sanitized;

            // secure path
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new RuntimeException("Invalid file path");
            }

            // save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // prepare multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("resume", new FileSystemResource(filePath.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            // call ML API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlApiUrl + "/parse_resume",
                    request,
                    Map.class
            );

            Map<String, Object> mlData = response.getBody();

            // update user
            if (userId != null && mlData != null && mlData.get("skills") != null) {

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // delete old resume
                if (user.getResumePath() != null) {
                    Path oldPath = uploadPath.resolve(user.getResumePath());
                    Files.deleteIfExists(oldPath);
                }

                user.setResumePath(filename);

                // safe casting + normalization
                Object skillsObj = mlData.get("skills");
                if (skillsObj instanceof List<?>) {
                    List<String> skills = ((List<?>) skillsObj).stream()
                            .map(Object::toString)
                            .map(s -> s.toLowerCase().trim())
                            .collect(Collectors.toList());

                    user.setSkills(skills);
                }

                userRepository.save(user);
            }

            return mlData;

        } catch (Exception err) {
            throw handleError(err, "resume upload");
        }
    }

    // =========================
    // ✅ Download Resume
    // =========================
    @Override
    public ResponseEntity<InputStreamResource> downloadResume(String filename) {

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (!filePath.startsWith(uploadPath) || !Files.exists(filePath)) {
                throw new RuntimeException("Resume not found");
            }

            InputStreamResource resource =
                    new InputStreamResource(new FileInputStream(filePath.toFile()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception err) {
            throw handleError(err, "resume download");
        }
    }

    // =========================
    // ✅ Recommend Jobs
    // =========================
    @Override
    public Map<String, Object> recommendJobs(UUID userId) {

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getSkills() == null || user.getSkills().isEmpty()) {
                throw new RuntimeException("User skills not found");
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

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlApiUrl + "/match_jobs",
                    request,
                    Map.class
            );

            return response.getBody();

        } catch (Exception err) {
            throw handleError(err, "job recommendation");
        }
    }

    // =========================
    // ✅ File Validation (Multer Equivalent)
    // =========================
    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No file uploaded");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new RuntimeException("Invalid file name");
        }

        String filename = originalName.toLowerCase();
        String ext = filename.substring(filename.lastIndexOf("."));

        List<String> allowed = List.of(".pdf", ".doc", ".docx", ".txt");

        if (!allowed.contains(ext)) {
            throw new RuntimeException(
                    "Only PDF, DOC, DOCX and TXT files are allowed"
            );
        }
    }

    // =========================
    // ✅ Error Handler
    // =========================
    private RuntimeException handleError(Exception err, String context) {

        System.err.println("❌ " + context + " error: " + err.getMessage());

        if (err instanceof org.springframework.web.client.HttpStatusCodeException ex) {
            return new RuntimeException(
                    "Request failed (API error): " + ex.getResponseBodyAsString()
            );
        } else if (err instanceof org.springframework.web.client.ResourceAccessException) {
            return new RuntimeException("No response from service");
        } else {
            return new RuntimeException(context + " failed: " + err.getMessage());
        }
    }
}