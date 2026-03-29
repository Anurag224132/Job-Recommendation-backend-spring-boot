package com.example.job_recommendation_backend.repository.projection;

public interface PlatformMetrics {
    long getTotalUsers();
    long getTotalRecruiters();
    long getTotalStudents();
    long getTotalJobs();
}
