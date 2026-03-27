package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    
    List<Interview> findAllByUserId(UUID userId);
    
    List<Interview> findAllByJobId(UUID jobId);

    @Query(value = "SELECT * FROM interviews WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<Interview> findAllDeletedInterviews();
}
