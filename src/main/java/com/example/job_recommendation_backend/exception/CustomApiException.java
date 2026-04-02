package com.example.job_recommendation_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomApiException extends RuntimeException {

    private final HttpStatus status;

    public CustomApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public CustomApiException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

}
