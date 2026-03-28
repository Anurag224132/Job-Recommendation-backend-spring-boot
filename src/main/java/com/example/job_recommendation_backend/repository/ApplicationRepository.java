package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.DTO.ApplicationResponseDto;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.repository.projection.StudentAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query("""
            SELECT
                COUNT(a) as jobsApplied,
                COALESCE(SUM(CASE WHEN a.status = 'rejected' THEN 1 ELSE 0 END), 0) as jobsRejected,
                (
                    SELECT COUNT(i)
                    FROM Interview i
                    WHERE i.user.id = :userId
                      AND i.status = 'completed'
                      AND i.deletedAt IS NULL
                ) as interviewsCompleted
            FROM Application a
            WHERE a.user.id = :userId
            """)
    StudentAnalytics getStudentAnalytics(UUID userId);

    @Query("""
            SELECT new com.example.job_recommendation_backend.DTO.ApplicationResponseDto(
                a.id,
                u.name,
                j.title,
                j.companyName,
                a.status,
                a.fitScore,
                a.createdAt
            )
            FROM Application a
            JOIN a.user u
            JOIN a.job j
            """)
    Page<ApplicationResponseDto> findAllApplications(Pageable pageable);
}
