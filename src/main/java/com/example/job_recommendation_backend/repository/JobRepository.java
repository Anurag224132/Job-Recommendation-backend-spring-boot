package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    void detachJobsFromUser(UUID userId);

    Page<Job> findByTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(String title, String companyName,
            Pageable pageable);

    @Query(value = """
             SELECT
                 COALESCE((SELECT COUNT(*) FROM jobs WHERE user_id = :userId AND deleted_at IS NULL),0) as jobsPosted,
                 COALESCE((SELECT COUNT(*) FROM applications a JOIN jobs j ON a.job_id = j.id WHERE j.user_id = :userId AND a.deleted_at IS NULL),0) as totalApplications,
                 COALESCE((SELECT COUNT(*) FROM applications a JOIN jobs j ON a.job_id = j.id WHERE j.user_id = :userId AND a.status = 'rejected' AND a.deleted_at IS NULL),0) as rejected,
                 COALESCE((SELECT COUNT(*) FROM interviews i JOIN jobs j ON i.job_id = j.id WHERE j.user_id = :userId AND i.deleted_at IS NULL),0) as interviewsScheduled
            """, nativeQuery = true)
    Object[] getRecruiterAnalytics(@Param("userId") UUID userId);
}
