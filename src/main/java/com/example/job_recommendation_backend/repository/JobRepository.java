package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
}
