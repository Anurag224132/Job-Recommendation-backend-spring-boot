package com.example.job_recommendation_backend.repository;

import ch.qos.logback.core.status.Status;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.enums.ApplicationStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    
    List<Application> findAllByUserId(UUID userId);
    
    List<Application> findAllByJobId(UUID jobId);

    @Query(value = "SELECT * FROM applications WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<Application> findAllDeletedApplications();

    @Query(" SELECT COUNT(ap) FROM Application ap WHERE ap.user.id = :userId ")
    long countApplicationsByUserId(@Param("userId") UUID userId);

    @Query(" SELECT COUNT(ap) FROM Application ap WHERE ap.user.id = :userId and ap.status = :status")
    long countApplicationsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ApplicationStatus status);
    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.user.id = :userId")
    long countApplicationsByRecruiterId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.user.id = :userId AND a.status = :status")
    long countApplicationsByRecruiterIdAndStatus(@Param("userId") UUID userId, @Param("status") ApplicationStatus status);
}
