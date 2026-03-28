package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Job;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.user = null WHERE j.user.id = :userId")
    void detachJobsFromUser(UUID userId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.user.id = :userId")
    long getCountOfJobGivenRecruiter(@Param("userId") UUID userId);

    @Query(value = "SELECT " +
            "COUNT(DISTINCT j.id) as jobsPosted, " +
            "COUNT(ap.id) as totalApplications, " +
            "SUM(CASE WHEN ap.status = 'rejected' THEN 1 ELSE 0 END) as rejected, " +
            "SUM(CASE WHEN ap.status = 'interview_scheduled' THEN 1 ELSE 0 END) as interviewsScheduled " +
            "FROM jobs j " +
            "LEFT JOIN applications ap ON j.id = ap.job_id " +
            "WHERE j.user_id = :userId", nativeQuery = true)
    Object[] getRecruiterAnalytics(@Param("userId") UUID userId);
}
