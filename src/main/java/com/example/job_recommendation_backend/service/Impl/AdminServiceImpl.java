package com.example.job_recommendation_backend.service.impl;

import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.DTO.PlatformMetricsDto;
import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    public Page<UserResponseDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponseDto> users = userRepository.findAllUsers(pageable);
        return users;
    }

    public String deleteUser(UUID id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with given id");
        }

        User user = userOpt.get();

        userRepository.delete(user);
        return "User deleted successfully";

    }

    public String changeRole(UUID id, Role role) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with given id");
        }

        User user = userOpt.get();

        if (user.getRole() == Role.recruiter && role != Role.recruiter) {
            jobRepository.detachJobsFromUser(id);
        }

        user.setRole(role);
        userRepository.save(user);
        return "User updated successfully";
    }

    public Page<JobResponseDto> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }

    public String deleteJob(UUID id) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            throw new RuntimeException("Job not found with given id");
        }

        Job job = jobOpt.get();
        jobRepository.delete(job);
        return "job deleted successfully";
    }

    public PlatformMetricsDto getPlatformMetrics() {
        Map<String, Long> metrics = userRepository.getPlatformMetrics();

        return PlatformMetricsDto.builder()
                .totalUsers(metrics.getOrDefault("totalusers", 0L))
                .totalRecruiters(metrics.getOrDefault("totalrecruiters", 0L))
                .totalStudents(metrics.getOrDefault("totalstudents", 0L))
                .totalJobs(metrics.getOrDefault("totaljobs", 0L))
                .build();
    }

    private JobResponseDto mapToResponseDto(Job job) {
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
                .recruiter(JobResponseDto.RecruiterDto.builder()
                        .name(job.getUser().getName())
                        .email(job.getUser().getEmail())
                        .build())
                .build();
    }
}
