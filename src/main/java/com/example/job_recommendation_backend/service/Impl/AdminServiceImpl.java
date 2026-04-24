package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.JobResponseDto;
import com.example.job_recommendation_backend.DTO.PlatformMetricsDto;
import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.projection.*;
import com.example.job_recommendation_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import com.example.job_recommendation_backend.DTO.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private InterviewService interviewService;

    @Override
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @Override
    public String deleteUser(UUID id) {
        return userService.deleteUser(id);
    }

    @Override
    public String deleteJob(UUID jobId, UUID userId) {
        return jobService.deleteJob(jobId,userId);
    }

    @Override
    public String deleteApplication(UUID applicationId, Role role,UUID userId) {
        return applicationService.deleteApplication(applicationId,role,userId);
    }

    @Override
    public String deleteInterview(UUID id) {
        return interviewService.deleteInterview(id);
    }

    @Override
    public String changeRole(UUID id, Role role) {
        User user = userService.getUserById(id);

        if (user.getRole() == role) {
            return "Role is already " + role;
        }
        if (user.getRole() == Role.recruiter && role != Role.recruiter) {
            int dlt=jobRepository.softDeleteJobsByUser(id);
        }

        user.setRole(role);
        userService.updateUser(user);
        return "User role updated successfully";
    }

    @Override
    public Page<JobResponseDto> getAllJobs(Pageable pageable) {
        return jobService.getAllJobs(pageable);
    }

    @Override
    public PlatformMetricsDto getPlatformMetrics() {
        PlatformMetrics metrics = userService.getPlatformMetrics();

        return PlatformMetricsDto.builder()
                .totalUsers(metrics.getTotalUsers())
                .totalRecruiters(metrics.getTotalRecruiters())
                .totalStudents(metrics.getTotalStudents())
                .totalJobs(metrics.getTotalJobs())
                .build();
    }

    @Override
    public AnalyticsCardDto getAnalyticsCard(String range) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = getStartDate(range, now);

        AnalyticsCounts counts = userService.getAnalyticsCounts();

        List<UserActivity> activityData =userService.getUserActivityDay(startDate,now);

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

    @Override
    public UserAnalyticsDto getUserAnalytics(UUID userId) {

        User user = userService.getUserById(userId);

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

            StudentAnalytics stats = applicationService.getStudentAnalytics(userId);

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

    @Override
    public Page<UserResponseDto> searchUsers(String query, Pageable pageable) {
        return userService.searchUsers(query, pageable);
    }

    @Override
    public Page<JobResponseDto> searchJobs(String query, Pageable pageable) {
        return jobService.searchJobs(query, null, null, null, null, null, pageable);
    }

    @Override
    public JobResponseDto getJobDetails(UUID id) {
        return jobService.getJobById(id);
    }

    @Override
    public String toggleJobStatus(UUID id) {
        jobService.toggleJobStatus(id);
        return "Job status toggled successfully";
    }

    @Override
    public Page<ApplicationResponseDto> getAllApplications(UUID userId,Role role,Pageable pageable) {
        return applicationService.allApplications(userId, role , pageable);
    }

    @Override
    public Page<InterviewResponseDto> getAllInterviews(Pageable pageable) {
        return interviewService.getAllInterviews(pageable);
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
        return (int) Math.round((completed * 100.0) / checks.size());
    }

    private boolean isValidField(String field) {
        return field != null && !field.isBlank();
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
                    // DB returns 3-char short names (Mon, Tue, etc.)
                    long count = activityMap.getOrDefault(day.substring(0, 3).toLowerCase(), 0L);
                    return new UserActivityDto(day.substring(0, 3), count);
                })
                .toList();
    }

    private LocalDateTime getStartDate(String range, LocalDateTime now) {
        if (range == null) {
            return now.minusDays(7);
        }
        return switch (range.toLowerCase()) {
            case "week" -> now.minusDays(7);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> now.minusDays(7);
        };
    }

}
