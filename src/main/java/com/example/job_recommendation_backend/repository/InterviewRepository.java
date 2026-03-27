package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
}
