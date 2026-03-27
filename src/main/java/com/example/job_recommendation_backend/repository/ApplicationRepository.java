package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Application;
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
}
