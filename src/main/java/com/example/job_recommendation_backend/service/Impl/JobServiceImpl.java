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
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.JobService;
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
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Job> getAllActiveJobs(Pageable pageable) {
        return jobRepository.findByIsActiveTrueAndDeletedAtIsNull(pageable);
    }

    @Override
    @Transactional
    public Job createJob(CreateJobRequestDto request, UUID userId){
        Role role= UserContext.get().getRole();
        if(role != Role.recruiter){
            throw new CustomApiException(HttpStatus.FORBIDDEN, "You are not allowed to perform this action");
        }
        User user= userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User", "id", userId.toString()));

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
    @Transactional(readOnly = true)
    public Page<JobResponseDto> getJobsByRecruiter(UUID userId,Pageable pageable) {
        Role role=UserContext.get().getRole();
        if(role != Role.recruiter){
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Only recruiters are allowed to perform this action");
        }
        Page<Job> jobs = jobRepository.findByRecruiterId(userId,pageable);
        return jobs.map(this::mapJobToResponseDto);
    }

    @Override
    @Transactional
    public void deleteJob(UUID jobId, UUID userId) {
        Role role=UserContext.get().getRole();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId.toString()));

        if (role != Role.recruiter || !job.getUser().getId().equals(userId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized to delete this job.");
        }

        job.setDeletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getUserApplications(UUID userId) {

        List<Application> applications =
                applicationRepository.findByUserIdWithJobAndUser(userId);

        return applications.stream()
                .filter(app -> app.getJob() != null) // safety check
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponseDto getJobById(UUID jobId){

        Job job = jobRepository.findById(jobId).orElseThrow(()-> new ResourceNotFoundException("Job", "id", jobId.toString()));

        return mapJobToResponseDto(job);
    }

    @Override
    public Page<JobResponseDto> searchJobs(String q, String location, Boolean remote, String type, String experience, String skills, Pageable pageable) {
        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (q != null && !q.isEmpty()) {
                String searchPattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.get("companyName")), searchPattern)
                ));
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

        RecruiterDto recruiter = job.getUser() == null ? null :
                RecruiterDto.builder()
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
