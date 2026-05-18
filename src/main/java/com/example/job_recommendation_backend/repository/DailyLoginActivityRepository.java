package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.entity.DailyLoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Repository
public interface DailyLoginActivityRepository extends JpaRepository<DailyLoginActivity, LocalDate> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO daily_login_activity (date, login_count) VALUES (:date, 1) " +
                   "ON CONFLICT (date) DO UPDATE SET login_count = daily_login_activity.login_count + 1", 
           nativeQuery = true)
    void incrementLoginCount(LocalDate date);
}
