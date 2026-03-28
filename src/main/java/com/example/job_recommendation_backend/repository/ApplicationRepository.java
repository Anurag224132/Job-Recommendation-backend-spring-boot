package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.enums.ApplicationStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query(" SELECT COUNT(ap) FROM Application ap WHERE ap.user.id = :userId ")
    long countApplicationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ap) FROM Application ap WHERE ap.user.id = :userId and ap.status = :status")
    long countApplicationsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ApplicationStatus status);
}
