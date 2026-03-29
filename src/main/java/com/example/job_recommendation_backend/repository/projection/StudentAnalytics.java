package com.example.job_recommendation_backend.repository.projection;

public interface StudentAnalytics {
    long getJobsApplied();
    long getJobsRejected();
    long getInterviewsCompleted();
}
