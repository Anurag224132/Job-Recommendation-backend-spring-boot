package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.DTO.InterviewResponseDto;
import com.example.job_recommendation_backend.entity.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {


    @Query("""
            SELECT new com.example.job_recommendation_backend.DTO.InterviewResponseDto(
                i.id,
                u.name,
                j.title,
                i.status,
                i.score,
                i.createdAt
            )
            FROM Interview i
            JOIN i.user u
            JOIN i.job j
            """)
    Page<InterviewResponseDto> findAllInterviews(Pageable pageable);
}
