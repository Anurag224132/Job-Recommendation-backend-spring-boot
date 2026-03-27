package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.Entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
}
