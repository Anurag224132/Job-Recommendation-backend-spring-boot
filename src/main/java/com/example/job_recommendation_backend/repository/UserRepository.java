package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.repository.projection.AnalyticsCounts;
import com.example.job_recommendation_backend.repository.projection.PlatformMetrics;
import com.example.job_recommendation_backend.repository.projection.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT new com.example.job_recommendation_backend.DTO.UserResponseDto(u.id, u.name, u.email, u.role) FROM User u")
    Page<UserResponseDto> findAllUsers(Pageable pageable);

    @Query(value = """
                SELECT
                    COUNT(*) as totalUsers,
                    COALESCE(SUM(CASE WHEN role = 'recruiter' THEN 1 ELSE 0 END),0) as totalRecruiters,
                    COALESCE(SUM(CASE WHEN role = 'student' THEN 1 ELSE 0 END), 0) as totalStudents,
                    (SELECT COUNT(*) FROM jobs WHERE deleted_at IS NULL) as totalJobs
                FROM users
                WHERE deleted_at IS NULL
            """, nativeQuery = true)
    PlatformMetrics getPlatformMetrics();

    @Query(value = "SELECT trim(to_char(d.date, 'Dy')) as day, COALESCE(SUM(d.login_count), 0) as count " +
            "FROM daily_login_activity d " +
            "WHERE d.date BETWEEN CAST(:startDate AS date) AND CAST(:endDate AS date) " +
            "GROUP BY extract(DOW from d.date), day " +
            "ORDER BY extract(DOW from d.date)",
            nativeQuery = true)
    List<UserActivity> getUserActivityByDay(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT new com.example.job_recommendation_backend.DTO.UserResponseDto(u.id, u.name, u.email, u.role)
            FROM User u
            WHERE (
                LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            """)
    Page<UserResponseDto> searchUsers(@Param("query") String query, Pageable pageable);

    @Query(value = """
                SELECT
                    (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL) as totalUsers,
                    (SELECT COUNT(*) FROM users WHERE role = 'student' AND deleted_at IS NULL) as studentCount,
                    (SELECT COUNT(*) FROM users WHERE role = 'recruiter' AND deleted_at IS NULL) as recruiterCount,
                    (SELECT COUNT(*) FROM users WHERE role = 'admin' AND deleted_at IS NULL) as adminCount,
                    (SELECT COUNT(*) FROM jobs WHERE deleted_at IS NULL) as totalJobs,
                    (SELECT COUNT(*) FROM applications WHERE deleted_at IS NULL) as totalApplications,
                    (SELECT COUNT(*) FROM interviews WHERE deleted_at IS NULL) as totalInterviews
            """, nativeQuery = true)
    AnalyticsCounts getAnalyticsCounts();
}
