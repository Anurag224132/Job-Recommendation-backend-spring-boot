package com.example.job_recommendation_backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponseDto {
    private int status;
    private String message;
    private boolean success;

    public ApiResponseDto(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.status = success ? 200 : 400;
    }

    public ApiResponseDto(String message, boolean success, int status) {
        this.message = message;
        this.success = success;
        this.status = status;
    }
}
