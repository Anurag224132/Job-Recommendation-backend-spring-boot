package com.example.job_recommendation_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "daily_login_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyLoginActivity {
    @Id
    private LocalDate date;
    private long loginCount;
}
