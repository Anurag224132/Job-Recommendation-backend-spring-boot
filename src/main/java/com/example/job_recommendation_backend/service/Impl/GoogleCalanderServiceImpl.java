package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.service.GoogleCalendarService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleCalanderServiceImpl implements GoogleCalendarService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String createMeetLink(String accessToken, LocalDateTime startTime) {

        String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events?conferenceDataVersion=1";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();

        body.put("summary", "Interview");
        body.put("description", "Interview with candidate");

        Map<String, String> start = Map.of(
                "dateTime", startTime.toString()
        );

        Map<String, String> end = Map.of(
                "dateTime", startTime.plusHours(1).toString()
        );

        body.put("start", start);
        body.put("end", end);

        Map<String, Object> conferenceData = Map.of(
                "createRequest", Map.of(
                        "requestId", UUID.randomUUID().toString()
                )
        );

        body.put("conferenceData", conferenceData);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> responseBody = response.getBody();

        return (String) responseBody.get("hangoutLink");
    }
}
