package com.example.job_recommendation_backend.repository.projection;

public interface AnalyticsCounts {
    long getTotalUsers();
    long getStudentCount();
    long getRecruiterCount();
    long getAdminCount();
    long getTotalJobs();
    long getTotalApplications();
}
