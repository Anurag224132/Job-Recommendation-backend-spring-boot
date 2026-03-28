package com.example.job_recommendation_backend.repository;

import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("SELECT new com.example.job_recommendation_backend.DTO.UserResponseDto(u.id, u.name, u.email, u.role) FROM User u")
    Page<UserResponseDto> findAllUsers(Pageable pageable);

    @Query(value = "SELECT * FROM users WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<User> findAllDeletedUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE (:role IS NULL OR u.role = :role)")
    long countUsers(@Param("role") Role role);

    @Query(value = """
                SELECT
                    COUNT(*) as totalUsers,
                    SUM(CASE WHEN role = 'recruiter' THEN 1 ELSE 0 END) as totalRecruiters,
                    SUM(CASE WHEN role = 'student' THEN 1 ELSE 0 END) as totalStudents,
                    (SELECT COUNT(*) FROM jobs WHERE deleted_at IS NULL) as totalJobs
                FROM users
                WHERE deleted_at IS NULL
            """, nativeQuery = true)
    Map<String, Long> getPlatformMetrics();

    @Query(value = "SELECT trim(to_char(ula.login_timestamp, 'Day')) as day, COUNT(*) as count " +
            "FROM user_login_activity ula " +
            "JOIN users u ON ula.user_id = u.id " +
            "WHERE ula.login_timestamp BETWEEN :startDate AND :endDate " +
            "AND u.deleted_at IS NULL " +
            "GROUP BY extract(DOW from ula.login_timestamp), day " +
            "ORDER BY extract(DOW from ula.login_timestamp)", 
            nativeQuery = true)
    List<Map<String, Object>> getUserActivityByDay(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
}
