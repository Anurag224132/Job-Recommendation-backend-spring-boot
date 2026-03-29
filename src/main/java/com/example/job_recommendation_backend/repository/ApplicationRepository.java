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
                    COALESCE(SUM(CASE 
                        WHEN a.status = com.example.job_recommendation_backend.enums.ApplicationStatus.rejected 
                        THEN 1 ELSE 0 END), 0) as jobsRejected,
                    COALESCE(SUM(CASE 
                        WHEN i.status = com.example.job_recommendation_backend.enums.InterviewStatus.completed 
                        THEN 1 ELSE 0 END), 0) as interviewsCompleted
                FROM Application a
                LEFT JOIN Interview i ON i.user.id = a.user.id
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
