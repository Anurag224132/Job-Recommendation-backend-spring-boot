package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.ApplicationResponseDto;
import com.example.job_recommendation_backend.DTO.CreateJobRequestDto;
import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.DTO.RecruiterDto;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.repository.ApplicationRepository;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.projection.RecruiterAnalytics;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.JobService;
import com.example.job_recommendation_backend.service.UserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.job_recommendation_backend.exception.CustomApiException;
import com.example.job_recommendation_backend.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public Page<JobResponseDto> getAllJobs(Pageable pageable) {
        Role role = UserContext.get().getRole();
        Page<Job> jobs;

        if (role == Role.admin) {
            jobs = jobRepository.findAllWithUser(pageable);
        } else {
            jobs = jobRepository.findByIsActiveTrueAndDeletedAtIsNull(pageable);
        }

        return jobs.map(this::mapJobToResponseDto);
    }

    @Override
    public Job createJob(CreateJobRequestDto request, UUID userId) {
        Role role = UserContext.get().getRole();
        if (role != Role.recruiter) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "You are not allowed to perform this action");
        }
        User user = userService.getUserById(userId);

        Job job = Job.builder()
                .title(request.getTitle())
                .experience(request.getExperience())
                .description(request.getDescription())
                .user(user)
                .companyName(request.getCompanyName())
                .isActive(true)
                .location(request.getLocation())
                .remote(request.getRemote())
                .salary(request.getSalary())
                .type(request.getType())
                .requiredSkills(request.getRequiredSkills())
                .build();
        jobRepository.save(job);
        return job;
    }

    @Override
    public Page<JobResponseDto> getJobsByRecruiter(UUID userId, Pageable pageable) {
        Role role = UserContext.get().getRole();
        if (role != Role.recruiter) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Only recruiters are allowed to perform this action");
        }
        Page<Job> jobs = jobRepository.findByRecruiterId(userId, pageable);
        return jobs.map(this::mapJobToResponseDto);
    }

    @Override
    public String deleteJob(UUID jobId, UUID userId) {
        Role role = UserContext.get().getRole();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (role != Role.admin && (role != Role.recruiter || !job.getUser().getId().equals(userId))) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized to delete this job.");
        }

        job.setDeletedAt(LocalDateTime.now());
        jobRepository.save(job);
        return "Job deleted successfully";
    }

    @Override
    public Job findById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id.toString()));
    }

    @Override
    public List<Job> findAllByIds(List<UUID> jobIds) {
        return jobRepository.findAllById(jobIds);
    }

    @Override
    public void deleteAllJobsByUserId(UUID id) {
        jobRepository.softDeleteJobsByUser(id);
    }

    @Override
    public Job updateJob(Job job) {
        try {
            return jobRepository.save(job);
        } catch (Exception e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update job: " + e.getMessage());
        }
    }

    @Override
    public RecruiterAnalytics getRecruiterStats(UUID id) {
        return jobRepository.getRecruiterAnalytics(id);
    }

    @Override
    @Transactional
    public boolean toggleJobStatus(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        Role role = UserContext.get().getRole();
        UUID currentUserId = UserContext.get().getUserId();

        if (role != Role.admin && !job.getUser().getId().equals(currentUserId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized to toggle this job");
        }

        job.setIsActive(!job.getIsActive());
        jobRepository.save(job);

        return job.getIsActive();
    }

    @Override
    public List<ApplicationResponseDto> getUserApplications(UUID userId) {

        List<Application> applications = applicationRepository.findByUserId(userId);

        return applications.stream()
                .filter(app -> app.getJob() != null) // safety check
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public JobResponseDto getJobById(UUID jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        return mapJobToResponseDto(job);
    }

    @Override
    public Page<JobResponseDto> searchJobs(String q, String location, Boolean remote, String type, String experience,
            String skills, Pageable pageable) {
        Role role = UserContext.get().getRole();

        Specification<Job> spec = (root, query, cb) -> {

            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("user", jakarta.persistence.criteria.JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (role != Role.admin) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (q != null && !q.isEmpty()) {
                String searchPattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.get("companyName")), searchPattern)));
            }

            if (location != null && !location.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }

            if (remote != null) {
                predicates.add(cb.equal(root.get("remote"), remote));
            }

            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (experience != null && !experience.isEmpty()) {
                predicates.add(cb.equal(root.get("experience"), experience));
            }

            if (skills != null && !skills.isEmpty()) {
                List<String> skillList = Arrays.stream(skills.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                for (String skill : skillList) {
                    predicates.add(cb.isMember(skill.trim().toLowerCase(), root.get("requiredSkills")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Job> jobPage = jobRepository.findAll(spec, pageable);
        return jobPage.map(this::mapJobToResponseDto);
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
                .recruiter(recruiter)
                .build();
    }

    private ApplicationResponseDto mapToDto(Application app) {

        return ApplicationResponseDto.builder()
                .id(app.getId())
                .studentName(app.getUser().getName())
                .jobTitle(app.getJob().getTitle())
                .companyName(app.getJob().getCompanyName())
                .status(app.getStatus())
                .fitScore(app.getFitScore() != null ? app.getFitScore().toString() : null)
                .createdAt(app.getCreatedAt())
                .build();
    }

}
