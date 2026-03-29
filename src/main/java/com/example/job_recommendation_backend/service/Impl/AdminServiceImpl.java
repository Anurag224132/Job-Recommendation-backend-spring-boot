package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.DTO.PlatformMetricsDto;
import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.entity.Interview;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.ApplicationStatus;
import com.example.job_recommendation_backend.enums.InterviewStatus;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.repository.InterviewRepository;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.repository.projection.*;
import com.example.job_recommendation_backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
        return "User deleted successfully";
    }

    public String changeRole(UUID id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == role) {
            return "Role is already " + role;
        }
        if (user.getRole() == Role.recruiter && role != Role.recruiter) {
            jobRepository.detachJobsFromUser(id);
        }

        user.setRole(role);

        return "User role updated successfully";
    }

    public Page<JobResponseDto> getAllJobs(Pageable pageable) {
//        return jobRepository.findAllJobs(pageable);
        Page<Job> jobs = jobRepository.findAllWithUser(pageable);

        return jobs.map(job -> JobResponseDto.builder()
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
                .recruiter(
                        RecruiterDto.builder()
                                .name(job.getUser().getName())
                                .email(job.getUser().getEmail())
                                .build()
                )
                .build());
    }

    public String deleteJob(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new RuntimeException("Job not found");
        }

        jobRepository.deleteById(id);
        return "Job deleted successfully";
    }

    public PlatformMetricsDto getPlatformMetrics() {
        PlatformMetrics metrics = userRepository.getPlatformMetrics();

        return PlatformMetricsDto.builder()
                .totalUsers(metrics.getTotalUsers())
                .totalRecruiters(metrics.getTotalRecruiters())
                .totalStudents(metrics.getTotalStudents())
                .totalJobs(metrics.getTotalJobs())
                .build();
    }

    public AnalyticsCardDto getAnalyticsCard(String range) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = getStartDate(range, now);

        AnalyticsCounts counts = userRepository.getAnalyticsCounts();

        List<UserActivity> activityData =
                userRepository.getUserActivityByDay(startDate, now);

        List<UserActivityDto> userActivity = mapActivity(activityData);

        return AnalyticsCardDto.builder()
                .totalUsers(counts.getTotalUsers())
                .activeJobs(counts.getTotalJobs())
                .totalApplications(counts.getTotalApplications())
                .totalCourses(0)
                .studentCount(counts.getStudentCount())
                .recruiterCount(counts.getRecruiterCount())
                .adminCount(counts.getAdminCount())
                .userActivity(userActivity)
                .build();
    }

    private List<UserActivityDto> mapActivity(List<UserActivity> activityData) {

        List<String> days = Arrays.asList(
                "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday", "Sunday"
        );

        Map<String, Long> activityMap = activityData.stream()
                .collect(Collectors.toMap(
                        a -> a.getDay().trim().toLowerCase(),
                        UserActivity::getCount
                ));

        return days.stream()
                .map(day -> {
                    long count = activityMap.getOrDefault(day.toLowerCase(), 0L);
                    return new UserActivityDto(day.substring(0, 3), count);
                })
                .toList();
    }

    private LocalDateTime getStartDate(String range, LocalDateTime now) {
        return switch (range.toLowerCase()) {
            case "week" -> now.minusDays(7);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> LocalDateTime.of(1970, 1, 1, 0, 0);
        };
    }

    public UserAnalyticsDto getUserAnalytics(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserAnalyticsDto.UserAnalyticsDtoBuilder builder = UserAnalyticsDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .lastActive(
                        user.getLastLogin().isEmpty()
                                ? user.getUpdatedAt()
                                : user.getLastLogin().get(user.getLastLogin().size() - 1))
                .createdAt(user.getCreatedAt())
                .skills(user.getSkills())
                .profileCompleted(calculateProfileCompletion(user));

        if (user.getRole() == Role.student) {

            StudentAnalytics stats = applicationRepository.getStudentAnalytics(userId);

            long jobsApplied = stats.getJobsApplied();
            long jobsRejected = stats.getJobsRejected();
            long interviewed = stats.getInterviewsCompleted();

            builder.coursesEnrolled(0L)
                    .jobsApplied(jobsApplied)
                    .jobsRejected(jobsRejected)
                    .jobsInterviewed(interviewed)
                    .rejectionRate(jobsApplied > 0 ? (int) (jobsRejected * 100 / jobsApplied) : 0)
                    .interviewRate(jobsApplied > 0 ? (int) (interviewed * 100 / jobsApplied) : 0);

        } else if (user.getRole() == Role.recruiter) {

            RecruiterAnalytics stats = jobRepository.getRecruiterAnalytics(userId);

            long totalApplications = stats.getTotalApplications();

            builder.jobsPosted((int) stats.getJobsPosted())
                    .totalApplications(totalApplications)
                    .rejected(stats.getRejected())
                    .interviewsScheduled(stats.getInterviewsScheduled())
                    .rejectionRate(totalApplications > 0
                            ? (int) (stats.getRejected() * 100 / totalApplications)
                            : 0)
                    .interviewRate(totalApplications > 0
                            ? (int) (stats.getInterviewsScheduled() * 100 / totalApplications)
                            : 0);
        }

        return builder.recentActivities(new ArrayList<>()).build();
    }

    public Page<UserResponseDto> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }

    public Page<JobResponseDto> searchJobs(String query, Pageable pageable) {
//        return jobRepository.searchJobs(query, pageable);
        Page<Job> jobs = jobRepository.searchJobsWithUser(query, pageable);

        return jobs.map(job -> JobResponseDto.builder()
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
                .recruiter(
                        RecruiterDto.builder()
                                .name(job.getUser().getName())
                                .email(job.getUser().getEmail())
                                .build()
                )
                .build());
    }

    public String toggleJobStatus(UUID id) {
        int updated = jobRepository.toggleJobStatus(id);
        if (updated == 0) {
            throw new RuntimeException("Job not found");
        }
        return "Job status toggled successfully";
    }

    public Page<ApplicationResponseDto> getAllApplications(Pageable pageable) {
        return applicationRepository.findAllApplications(pageable);
    }

    public Page<InterviewResponseDto> getAllInterviews(Pageable pageable) {
        return interviewRepository.findAllInterviews(pageable);
    }

    public String deleteApplication(UUID id) {

        if (!applicationRepository.existsById(id)) {
            throw new RuntimeException("Application not found");
        }
        applicationRepository.deleteById(id);
        return "Application deleted successfully";
    }

    public String deleteInterview(UUID id) {
        if (!interviewRepository.existsById(id)) {
            throw new RuntimeException("Interview not found");
        }
        interviewRepository.deleteById(id);
        return "Interview deleted successfully";
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

}
