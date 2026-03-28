package com.example.job_recommendation_backend.repository.projection;

public interface RecruiterAnalytics {

    long getJobsPosted();

    long getTotalApplications();

    long getRejected();

    long getInterviewsScheduled();
}
