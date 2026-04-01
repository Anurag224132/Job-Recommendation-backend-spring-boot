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

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    @Override
    public Map<String, Object> uploadResume(MultipartFile file, UUID userId) {

        try {
            validateFile(file);

            String originalName = file.getOriginalFilename();
            String sanitized = originalName.replaceAll("[^a-zA-Z0-9_.-]", "_");
            String filename = System.currentTimeMillis() + "-" + sanitized;

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new RuntimeException("Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("resume", new FileSystemResource(filePath.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlApiUrl + "/parse_resume",
                    request,
                    Map.class
            );

            Map<String, Object> mlData = response.getBody();

            if (userId != null && mlData != null && mlData.get("skills") != null) {

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));

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

    @Override
    public ResponseEntity<InputStreamResource> downloadResume(UUID userId) {

        try {
            UUID currentUserId = UserContext.get().getUserId();

            if (!currentUserId.equals(userId)) {
                throw new RuntimeException("Not authorized");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String filename = user.getResumePath();

            if (filename == null) {
                throw new RuntimeException("Resume not found");
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new RuntimeException("Invalid file path");
            }

            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found");
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(resource.getInputStream()));

        } catch (Exception e) {
            throw new RuntimeException("Error downloading file", e);
        }
    }

    @Override
    public Map<String, Object> recommendJobs(UUID userId) {

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getSkills() == null || user.getSkills().isEmpty()) {
                throw new RuntimeException("User skills not found");
            }

            // TODO : have to think again about this query this would be slow query in big db
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

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No file uploaded");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File too large");
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