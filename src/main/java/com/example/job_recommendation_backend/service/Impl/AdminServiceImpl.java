package com.example.job_recommendation_backend.service.impl;

import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.DTO.PlatformMetricsDto;
import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.ApplicationStatus;
import com.example.job_recommendation_backend.enums.InterviewStatus;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.repository.InterviewRepository;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.repository.ApplicationRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {

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

    public AnalyticsCardDto getAnalyticsCard(String range) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (range.toLowerCase()) {
            case "week":
                startDate = now.minusDays(7);
                break;
            case "month":
                startDate = now.minusMonths(1);
                break;
            case "year":
                startDate = now.minusYears(1);
                break;
            default:
                startDate = LocalDateTime.of(1970, 1, 1, 0, 0);
        }

        long totalUsers = userRepository.count();
        long totalJobs = jobRepository.count();
        long totalApplications = applicationRepository.count();
        long totalCourses = 0; // Course entity not yet implemented
        long studentCount = userRepository.countUsers(Role.student);
        long recruiterCount = userRepository.countUsers(Role.recruiter);
        long adminCount = userRepository.countUsers(Role.admin);

        // Get activity data
        List<Map<String, Object>> activityData = userRepository.getUserActivityByDay(startDate, now);

        // Map activity data to DTO
        List<String> days = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        List<UserActivityDto> userActivity = days.stream()
                .map(dayName -> {
                    long count = activityData.stream()
                            .filter(m -> dayName.equalsIgnoreCase(((String) m.get("day")).trim()))
                            .mapToLong(m -> ((Number) m.get("count")).longValue())
                            .findFirst()
                            .orElse(0L);

                    // Shorten name to match frontend expectations ("Mon", "Tue", etc)
                    String shortName = dayName.substring(0, 3);
                    return new UserActivityDto(shortName, count);
                })
                .collect(Collectors.toList());

        return AnalyticsCardDto.builder()
                .totalUsers(totalUsers)
                .activeJobs(totalJobs)
                .totalApplications(totalApplications)
                .totalCourses(totalCourses)
                .studentCount(studentCount)
                .recruiterCount(recruiterCount)
                .adminCount(adminCount)
                .userActivity(userActivity)
                .build();
    }

    public UserAnalyticsDto getUserAnalytics(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        UserAnalyticsDto.UserAnalyticsDtoBuilder builder = UserAnalyticsDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .lastActive((user.getLastLogin().isEmpty()) ? user.getUpdatedAt()
                        : user.getLastLogin().get(user.getLastLogin().size() - 1))
                .createdAt(user.getCreatedAt())
                .skills(user.getSkills())
                .profileCompleted(calculateProfileCompletion(user));

        if (user.getRole() == Role.student) {
            long jobsApplied = applicationRepository.countApplicationsByUserId(user.getId());
            long jobsRejected = applicationRepository.countApplicationsByUserIdAndStatus(user.getId(),
                    ApplicationStatus.rejected);
            long interviewed = interviewRepository.countInterviewByUserIdAndStatus(user.getId(),
                    InterviewStatus.completed);

            builder.coursesEnrolled(0L)
                    .jobsApplied(jobsApplied)
                    .jobsRejected(jobsRejected)
                    .jobsInterviewed(interviewed)
                    .rejectionRate(jobsApplied > 0 ? (int) Math.round((double) jobsRejected / jobsApplied * 100) : 0)
                    .interviewRate(jobsApplied > 0 ? (int) Math.round((double) interviewed / jobsApplied * 100) : 0);

        } else if (user.getRole() == Role.recruiter) {
            Object[] result = jobRepository.getRecruiterAnalytics(userId);

            long jobsPosted = ((Number) result[0]).longValue();
            long totalApplications = ((Number) result[1]).longValue();
            long rejected = ((Number) result[2]).longValue();
            long interviewsScheduled = ((Number) result[3]).longValue();

            builder.jobsPosted((int) jobsPosted)
                    .totalApplications(totalApplications)
                    .rejected(rejected)
                    .interviewsScheduled(interviewsScheduled)
                    .rejectionRate(
                            totalApplications > 0 ? (int) Math.round((double) rejected / totalApplications * 100) : 0)
                    .interviewRate(totalApplications > 0
                            ? (int) Math.round((double) interviewsScheduled / totalApplications * 100)
                            : 0);

        }

        return builder.recentActivities(new ArrayList<>()).build();
    }

    public Page<UserResponseDto> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable)
                .map(user -> new UserResponseDto(user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }

    public Page<JobResponseDto> searchJobs(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jobRepository.findByTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(query, query, pageable)
                .map(this::mapToResponseDto);
    }

    public String toggleJobStatus(UUID id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
        job.setIsActive(!job.getIsActive());
        jobRepository.save(job);
        return "Job status updated to " + (job.getIsActive() ? "Active" : "Inactive");
    }

    public Page<ApplicationResponseDto> getAllApplications(Pageable pageable) {
        return applicationRepository.findAll(pageable)
                .map(this::mapToApplicationResponseDto);
    }

    public Page<InterviewResponseDto> getAllInterviews(Pageable pageable) {
        return interviewRepository.findAll(pageable)
                .map(this::mapToInterviewResponseDto);
    }

    public String deleteApplication(UUID id) {
        com.example.job_recommendation_backend.entity.Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        applicationRepository.delete(app);
        return "Application deleted successfully";
    }

    public String deleteInterview(UUID id) {
        com.example.job_recommendation_backend.entity.Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
        interviewRepository.delete(interview);
        return "Interview deleted successfully";
    }

    private ApplicationResponseDto mapToApplicationResponseDto(com.example.job_recommendation_backend.entity.Application app) {
        return ApplicationResponseDto.builder()
                .id(app.getId())
                .studentName(app.getUser().getName())
                .jobTitle(app.getJob().getTitle())
                .companyName(app.getJob().getCompanyName())
                .status(app.getStatus())
                .fitScore(app.getFitScore())
                .createdAt(app.getCreatedAt())
                .build();
    }

    private InterviewResponseDto mapToInterviewResponseDto(com.example.job_recommendation_backend.entity.Interview interview) {
        return InterviewResponseDto.builder()
                .id(interview.getId())
                .studentName(interview.getUser().getName())
                .jobTitle(interview.getJob().getTitle())
                .status(interview.getStatus())
                .score(interview.getScore())
                .createdAt(interview.getCreatedAt())
                .build();
    }

    private int calculateProfileCompletion(User user) {
        int completed = 0;
        List<Boolean> checks = List.of(
                isValidField(user.getName()),
                user.getSkills() != null && !user.getSkills().isEmpty(),
                isValidField(user.getResumePath()),
                isValidField(user.getProfilePicture()),
                isValidField(user.getBio()));
        for (boolean check : checks) {
            if (check)
                completed++;
        }
        return (completed * 100) / checks.size();
    }

    private boolean isValidField(String field) {
        return field != null && !field.isBlank();
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
