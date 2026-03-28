package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.Job;
import io.lettuce.core.dynamic.annotation.Param;
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

    @Query(" Select count(*) from Job where Job.user.id = :userId")
    long getCountOfJobGivenRecruiter(@Param("userId") UUID userId);

    org.springframework.data.domain.Page<Job> findByTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(String title, String companyName, org.springframework.data.domain.Pageable pageable);
}
