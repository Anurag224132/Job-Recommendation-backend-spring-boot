package com.example.job_recommendation_backend.service.Impl;

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

@Service
@Slf4j
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

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    @Override
    public Map<String, Object> uploadResume(MultipartFile file, UUID userId) {

        try {
            validateFile(file);

            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid file name");
            }
            String sanitized = originalName.replaceAll("[^a-zA-Z0-9_.-]", "_");
            String filename = System.currentTimeMillis() + "-" + sanitized;

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("resume", new FileSystemResource(filePath.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            Map<String, Object> mlData = new HashMap<>();
            
            if (mlApiUrl != null && !mlApiUrl.isEmpty()) {
                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            mlApiUrl + "/parse_resume",
                            request,
                            Map.class
                    );
                    mlData = response.getBody();
                    log.info("📄 ML Resume Parse Result: {}", mlData);
                } catch (Exception e) {
                    log.warn("⚠️ ML Resume Parsing failed: {}. Continuing with file upload only.", e.getMessage());
                }
            } else {
                log.info("ℹ️ ML API URL not provided, skipping resume parsing.");
            }

            if (userId != null && mlData != null) {

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

                if (user.getResumePath() != null) {
                    Path oldPath = uploadPath.resolve(user.getResumePath());
                    Files.deleteIfExists(oldPath);
                }

                user.setResumePath(filename);

                Object skillsObj = mlData.get("skills");
                if (skillsObj instanceof List<?>) {
                    List<String> skills = ((List<?>) skillsObj).stream()
                            .map(Object::toString)
                            .map(s -> s.toLowerCase().trim())
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    log.info("✅ Extracted {} skills for user {}: {}", skills.size(), user.getEmail(), skills);
                    user.setSkills(skills);
                } else {
                    log.warn("⚠️ No skills list found in ML response for user {}", user.getEmail());
                }

                userRepository.save(user);
                log.info("💾 User profile updated successfully with new resume and skills");
            }

            return mlData;

        } catch (Exception err) {
            throw handleError(err, "resume upload");
        }
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadResume(UUID userId) {

        try {
            UUID currentUserId = UserContext.get().getUserId();

            if (!currentUserId.equals(userId)) {
                throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

            String filename = user.getResumePath();

            if (filename == null) {
                throw new CustomApiException(HttpStatus.NOT_FOUND, "Resume not found");
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }

            if (!Files.exists(filePath)) {
                throw new CustomApiException(HttpStatus.NOT_FOUND, "File not found");
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(resource.getInputStream()));

        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file");
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

            // TODO : have to think again about this query this would be slow query in big
            // db
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
                log.warn("ℹ️ ML API URL not provided, cannot recommend jobs via ML.");
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
                log.warn("⚠️ ML Job Recommendation failed: {}. Returning empty matches.", err.getMessage());
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