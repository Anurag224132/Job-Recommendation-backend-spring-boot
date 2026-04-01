package com.example.job_recommendation_backend.service;

import java.time.LocalDateTime;

public interface GoogleCalendarService {
    String createMeetLink(LocalDateTime startTime, String candidateEmail);
}
