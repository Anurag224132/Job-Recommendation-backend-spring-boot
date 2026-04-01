package com.example.job_recommendation_backend.security;

import com.example.job_recommendation_backend.enums.Role;

import java.util.UUID;

public class UserContext {

    private static final ThreadLocal<UserDetails> context = new ThreadLocal<>();

    public static void set(UUID userId, String token, Role role) {
        context.set(new UserDetails(userId, token, role));
    }

    public static UserDetails get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }

    public static class UserDetails {
        private final UUID userId;
        private final String token;
        private final Role role;

        public UserDetails(UUID userId, String token, Role role) {
            this.userId = userId;
            this.token = token;
            this.role = role;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getToken() {
            return token;
        }

        public Role getRole() {
            return role;
        }
    }
}