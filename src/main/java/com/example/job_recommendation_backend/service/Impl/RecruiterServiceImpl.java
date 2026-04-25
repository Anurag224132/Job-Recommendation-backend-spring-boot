package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.repository.ApplicationRepository;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.service.JobService;
import com.example.job_recommendation_backend.service.RecruiterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
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
@Slf4j
public class RecruiterServiceImpl implements RecruiterService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobService jobService;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/resumes";

    @Override
    public Map<String, List<JobAnalyticsDto>> getAnalytics(UUID userId) {
        List<JobAnalyticsDto> data = applicationRepository.getJobAnalytics(userId);
        return Map.of("applicationsPerJob", data);
    }

    @Override
    public SkillGapDto skillGapAnalysis(UUID jobId, UUID userId, Pageable pageable) {

        Job job = jobService.findById(jobId);

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
    public List<ApplicantDto> getJobApplicants(UUID jobId, UUID userId, Pageable pageable) {

        Job job = jobService.findById(jobId);

        if (!job.getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        Page<Application> apps = applicationRepository.findApplicantsByJobId(jobId, pageable);

        return apps.stream().map(a -> new ApplicantDto(
                a.getId(),
                a.getUser().getName(),
                a.getUser().getEmail(),
                a.getUser().getSkills(),
                a.getJob().getTitle())).toList();
    }

    @Override
    public JobResponseDto updateJob(UUID jobId, UUID userId, Map<String, Object> body) {
        Job job = jobService.findById(jobId);

        if (!job.getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized to update this job");
        }

        if (body.get("title") != null)
            job.setTitle(String.valueOf(body.get("title")));

        if (body.get("description") != null)
            job.setDescription(String.valueOf(body.get("description")));

        if (body.get("location") != null)
            job.setLocation(String.valueOf(body.get("location")));

        if (body.get("salary") != null)
            job.setSalary(String.valueOf(body.get("salary")));

        if (body.get("type") != null)
            job.setType(String.valueOf(body.get("type")));

        if (body.get("experience") != null)
            job.setExperience(String.valueOf(body.get("experience")));

        if (body.get("remote") != null) {
            Object remote = body.get("remote");
            job.setRemote(remote instanceof Boolean ? (Boolean) remote : Boolean.parseBoolean(remote.toString()));
        }

        if (body.get("isActive") != null) {
            Object active = body.get("isActive");
            job.setIsActive(active instanceof Boolean ? (Boolean) active : Boolean.parseBoolean(active.toString()));
        }

        if (body.get("companyName") != null)
            job.setCompanyName(String.valueOf(body.get("companyName")));

        if (body.get("requiredSkills") != null) {
            Object skillsObj = body.get("requiredSkills");
            if (skillsObj instanceof List<?>) {
                List<String> skills = ((List<?>) skillsObj)
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                job.setRequiredSkills(skills);
            }
        }

        job = jobService.updateJob(job);
        return mapJobToResponseDto(job);
    }

    private JobResponseDto mapJobToResponseDto(Job job) {
        RecruiterDto recruiter = job.getUser() == null ? null
                : RecruiterDto.builder()
                        .name(job.getUser().getName())
                        .email(job.getUser().getEmail())
                        .build();

        return JobResponseDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .location(job.getLocation())
                .salary(job.getSalary())
                .type(job.getType())
                .experience(job.getExperience())
                .remote(job.getRemote())
                .isActive(job.getIsActive())
                .companyName(job.getCompanyName())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .recruiter(recruiter)
                .build();
    }

    @Override
    public Map<String, Object> toggleJobActive(UUID jobId, UUID userId) {
        boolean active = jobService.toggleJobStatus(jobId);
        return Map.of("isActive", active);
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadResume(UUID appId, UUID userId) {

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
            InputStreamResource resource = new InputStreamResource(new FileInputStream(filePath.toFile()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + user.getName() + "_Resume.pdf\"")
                    .body(resource);

        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading resume");
        }
    }

    @Override
    public SkillGapDto getGlobalSkillGap(UUID userId) {
        // Fetch ALL jobs for this recruiter
        Page<Job> jobs = jobRepository.findByRecruiterId(userId, PageRequest.of(0, Integer.MAX_VALUE));

        Set<String> allRequiredSkills = jobs.stream()
                .filter(j -> j.getRequiredSkills() != null)
                .flatMap(j -> j.getRequiredSkills().stream())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Fetch ALL applications for this recruiter
        Page<Application> apps = applicationRepository.findAllByRecruiter(userId, PageRequest.of(0, Integer.MAX_VALUE));

        Set<String> applicantSkills = apps.stream()
                .filter(a -> a.getUser().getSkills() != null)
                .flatMap(a -> a.getUser().getSkills().stream())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> gaps = allRequiredSkills.stream()
                .filter(skill -> !applicantSkills.contains(skill))
                .distinct()
                .collect(Collectors.toList());

        return new SkillGapDto(gaps);
    }

    @Override
    public List<RecruiterDashboardDto> getRecruiterDashboard(UUID userId, Pageable pageable) {

        Page<Application> apps = applicationRepository.findAllByRecruiter(userId, pageable);

        // Todo: have to move aggregation on db level or paginate it for heavy
        // calculation
        Map<Job, List<Application>> grouped = apps.stream().collect(Collectors.groupingBy(Application::getJob));

        return grouped.entrySet().stream().map(entry -> {

            Job job = entry.getKey();

            List<ApplicantDto> applicants = entry.getValue().stream()
                    .map(a -> new ApplicantDto(
                            a.getId(),
                            a.getUser().getName(),
                            a.getUser().getEmail(),
                            a.getUser().getSkills(),
                            job.getTitle()))
                    .toList();

            return new RecruiterDashboardDto(
                    job.getId(),
                    job.getTitle(),
                    applicants);

        }).toList();
    }
}
