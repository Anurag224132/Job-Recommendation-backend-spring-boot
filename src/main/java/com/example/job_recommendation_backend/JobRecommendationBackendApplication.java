package com.example.job_recommendation_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JobRecommendationBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobRecommendationBackendApplication.class, args);
	}

}
