package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
