package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.repository.projection.RecruiterAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Job j SET j.user = null WHERE j.user.id = :userId")
    void detachJobsFromUser(@Param("userId") UUID userId);

    @Query(value = """
                SELECT
                    COALESCE((SELECT COUNT(*) FROM jobs WHERE user_id = :userId AND deleted_at IS NULL), 0) AS jobsPosted,
            
                    COALESCE((SELECT COUNT(*) 
                              FROM applications a 
                              JOIN jobs j ON a.job_id = j.id 
                              WHERE j.user_id = :userId AND a.deleted_at IS NULL), 0) AS totalApplications,
            
                    COALESCE((SELECT COUNT(*) 
                              FROM applications a 
                              JOIN jobs j ON a.job_id = j.id 
                              WHERE j.user_id = :userId AND a.status = 'rejected' AND a.deleted_at IS NULL), 0) AS rejected,
            
                    COALESCE((SELECT COUNT(*) 
                              FROM interviews i 
                              JOIN jobs j ON i.job_id = j.id 
                              WHERE j.user_id = :userId AND i.deleted_at IS NULL), 0) AS interviewsScheduled
            """, nativeQuery = true)
    RecruiterAnalytics getRecruiterAnalytics(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT j FROM Job j")
    Page<Job> findAllWithUser(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
                SELECT j FROM Job j
                WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Job> searchJobsWithUser(@Param("query") String query, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
                UPDATE Job j 
                SET j.isActive = 
                    CASE 
                        WHEN j.isActive = true THEN false 
                        ELSE true 
                    END
                WHERE j.id = :id
            """)
    int toggleJobStatus(@Param("id") UUID id);
}
