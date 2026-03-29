package com.example.job_recommendation_backend.service;

import java.time.LocalDateTime;

public interface GoogleCalendarService {
    public String createMeetLink(String accessToken, LocalDateTime startTime);
}
