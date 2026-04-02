package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.ApplicantDto;
import com.example.job_recommendation_backend.DTO.JobAnalyticsDto;
import com.example.job_recommendation_backend.DTO.RecruiterDashboardDto;
import com.example.job_recommendation_backend.DTO.SkillGapDto;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.repository.ApplicationRepository;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.service.RecruiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.job_recommendation_backend.exception.CustomApiException;
import com.example.job_recommendation_backend.exception.ResourceNotFoundException;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecruiterServiceImpl implements RecruiterService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/resumes";

    @Override
    public Map<String, List<JobAnalyticsDto>> getAnalytics(UUID userId) {
        List<JobAnalyticsDto> data = applicationRepository.getJobAnalytics(userId);
        return Map.of("applicationsPerJob", data);
    }

    @Override
    public SkillGapDto skillGapAnalysis(UUID jobId, UUID userId,Pageable pageable) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (!job.getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        Page<Application> applications = applicationRepository.findApplicantsByJobId(jobId, pageable);

        Set<String> required = job.getRequiredSkills() == null
                ? Set.of()
                : job.getRequiredSkills().stream()
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());

        Set<String> applicantSkills = applications.stream()
                .flatMap(a -> a.getUser().getSkills() == null
                        ? java.util.stream.Stream.empty()
                        : a.getUser().getSkills().stream())
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());

        List<String> missing = required.stream()
                .filter(skill -> !applicantSkills.contains(skill))
                .toList();

        return new SkillGapDto(missing);
    }

    @Override
    public List<ApplicantDto> getJobApplicants(UUID jobId, UUID userId,Pageable pageable) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (!job.getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        Page<Application> apps = applicationRepository.findApplicantsByJobId(jobId,pageable);

        return apps.stream().map(a ->
                new ApplicantDto(
                        a.getId(),
                        a.getUser().getName(),
                        a.getUser().getEmail(),
                        a.getUser().getSkills(),
                        a.getJob().getTitle()
                )
        ).toList();
    }

    @Override
    public Job updateJob(UUID jobId, UUID userId, Map<String, Object> body) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (!job.getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        if (body.get("title") != null)
            job.setTitle((String) body.get("title"));

        if (body.get("description") != null)
            job.setDescription((String) body.get("description"));

        if (body.get("requiredSkills") != null){
            Object skillsObj = body.get("requiredSkills");

            if (skillsObj instanceof List<?>) {
                List<String> skills = ((List<?>) skillsObj)
                        .stream()
                        .map(Object::toString)
                        .toList();
                job.setRequiredSkills(skills);
            }
        }

        return jobRepository.save(job);
    }

    @Override
    public Map<String, Object> toggleJobActive(UUID jobId, UUID userId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (!job.getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        job.setIsActive(!job.getIsActive());
        jobRepository.save(job);

        return Map.of("isActive", job.getIsActive());
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadResume(UUID appId,UUID userId) {

        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", appId.toString()));

        if (!app.getJob().getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        User user = app.getUser();

        if (user.getResumePath() == null) {
            throw new CustomApiException(HttpStatus.NOT_FOUND, "Resume not found");
        }

        try {

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(user.getResumePath()).normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }
            InputStreamResource resource =
                    new InputStreamResource(new FileInputStream(filePath.toFile()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + user.getName() + "_Resume.pdf\"")
                    .body(resource);

        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading resume");
        }
    }

    @Override
    public List<RecruiterDashboardDto> getRecruiterDashboard( UUID userId, Pageable pageable) {

        Page<Application> apps = applicationRepository.findAllByRecruiter(userId, pageable);

        //Todo: have to move aggregation on db level or paginate it for heavy calculation
        Map<Job, List<Application>> grouped =
                apps.stream().collect(Collectors.groupingBy(Application::getJob));

        return grouped.entrySet().stream().map(entry -> {

            Job job = entry.getKey();

            List<ApplicantDto> applicants = entry.getValue().stream()
                    .map(a -> new ApplicantDto(
                            a.getId(),
                            a.getUser().getName(),
                            a.getUser().getEmail(),
                            a.getUser().getSkills(),
                            job.getTitle()
                    )).toList();

            return new RecruiterDashboardDto(
                    job.getId(),
                    job.getTitle(),
                    applicants
            );

        }).toList();
    }
}
