package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.exception.CustomApiException;
import com.example.job_recommendation_backend.exception.ResourceNotFoundException;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.repository.projection.AnalyticsCounts;
import com.example.job_recommendation_backend.repository.projection.PlatformMetrics;
import com.example.job_recommendation_backend.repository.projection.UserActivity;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    public User getUserById(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        return user;
    }

    @Override
    public void updateUser(User user){
        try {
            userRepository.save(user);
        } catch(Exception e) {
        }
    }

    @Override
    public Page<UserResponseDto> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }

    @Override
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponseDto::fromEntity);
    }

    @Override
    public String deleteUser(UUID id) {
        Role role= UserContext.get().getRole();
        if(role !=Role.admin){
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized to do this action.");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        return "User deleted successfully";
    }

    public AnalyticsCounts getAnalyticsCounts(){
        return userRepository.getAnalyticsCounts();
    }

    public List<UserActivity> getUserActivityDay(LocalDateTime startDate,LocalDateTime now){
        return userRepository.getUserActivityByDay(startDate,now);
    }

    public PlatformMetrics getPlatformMetrics(){
        return userRepository.getPlatformMetrics();
    }
}
