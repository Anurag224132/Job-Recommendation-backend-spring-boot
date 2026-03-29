package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.DTO.ApplicationResponseDto;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.repository.projection.StudentAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
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

    @EntityGraph(attributePaths = {"user", "job"})
    @Query("SELECT ap FROM Application ap JOIN ap.job j WHERE j.user.id = :userId")
    Page<Application> findApplicationsForRecruiter(@Param("userId") UUID userId, Pageable pageable);


    @EntityGraph(attributePaths = {"user", "job"})
    @Query("select ap from Application ap join ap.user u where u.id = :userId")
    Page<Application> findApplicationsForStudent(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("""
                UPDATE Application ap 
                SET ap.deletedAt = CURRENT_TIMESTAMP
                WHERE ap.id = :applicationId
                AND ap.job.user.id = :userId
            """)
    int softDeleteByRecruiter(@Param("applicationId") UUID applicationId, @Param("userId") UUID userId);

    Optional<Application> findByUser_IdAndJob_Id(UUID userId, UUID jobId);

    boolean existsByUserIdAndJobIdAndDeletedAtIsNull(UUID userId, UUID jobId);
}
