package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Interview;
import com.example.job_recommendation_backend.enums.InterviewStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {


    @Query(" SELECT COUNT(interview) FROM Interview interview WHERE interview.user.id = :userId and interview.status = :status")
    long countInterviewByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") InterviewStatus status);
}
