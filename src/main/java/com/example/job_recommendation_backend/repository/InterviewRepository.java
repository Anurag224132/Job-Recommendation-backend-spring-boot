package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Interview;
import com.example.job_recommendation_backend.enums.ApplicationStatus;
import com.example.job_recommendation_backend.enums.InterviewStatus;
import io.lettuce.core.dynamic.annotation.Param;
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

    @Query(" SELECT COUNT(interview) FROM Interview interview WHERE interview.user.id = :userId and interview.status = :status")
    long countInterviewByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") InterviewStatus status);
}
