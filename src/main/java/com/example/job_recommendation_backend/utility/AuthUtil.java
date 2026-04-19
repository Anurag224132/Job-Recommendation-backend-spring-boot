package com.example.job_recommendation_backend.utility;

import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.security.UserContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthUtil {

    public UUID getCurrentUserId(){
        return UserContext.get().getUserId();
    }

    public Role getCurrentUserRole(){
        return UserContext.get().getRole();
    }
}
