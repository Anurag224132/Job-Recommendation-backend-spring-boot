package com.example.job_recommendation_backend.service.impl;

import com.example.job_recommendation_backend.DTO.LoginRequest;
import com.example.job_recommendation_backend.DTO.LoginResponse;
import com.example.job_recommendation_backend.DTO.RegisterUserDto;
import com.example.job_recommendation_backend.DTO.UserResponseDto;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.security.JwtUtil;
import com.example.job_recommendation_backend.service.AuthService;
import com.example.job_recommendation_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private EmailService emailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    private static final String OTP_SIGNUP_PREFIX = "OTP:SIGNUP:";
    private static final String OTP_FORGOT_PREFIX = "OTP:FORGOT:";
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Override
    public void initiateRegistration(RegisterUserDto registerUserDto) {
        if (userRepository.findByEmail(registerUserDto.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        sendAndStoreOtp(registerUserDto.getEmail(), registerUserDto.getName(), OTP_SIGNUP_PREFIX);
    }

    @Override
    public String verifyAndRegister(String email, String otp, RegisterUserDto registerUserDto) {
        String key = OTP_SIGNUP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new RuntimeException("OTP has expired or email is incorrect");
        }

        if (!storedOtp.equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        User user = User.builder()
                .name(registerUserDto.getName())
                .email(registerUserDto.getEmail())
                .password(passwordEncoder.encode(registerUserDto.getPassword()))
                .role(Role.valueOf(registerUserDto.getRole().toLowerCase()))
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().name());

        redisTemplate.delete(key);
        log.info("User registered successfully: {}", email);

        return token;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return new LoginResponse(token, UserResponseDto.fromEntity(user));
    }

    @Override
    public UserResponseDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponseDto.fromEntity(user);
    }

    @Override
    public void resendOtp(String email) {
        Optional<User> userOpt= userRepository.findByEmail(email);
        if(!userOpt.isPresent()){
            throw new RuntimeException("User does not exist with given email");
        }
        User user = userOpt.get();
        sendAndStoreOtp(email, user.getName(), OTP_SIGNUP_PREFIX);
    }

    @Override
    public void initiateForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        sendAndStoreOtp(email, user.getName(), OTP_FORGOT_PREFIX);
    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        String key = OTP_FORGOT_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password cannot be the same as the old password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete(key);
        log.info("Password reset successful for user: {}", email);
    }

    private void sendAndStoreOtp(String email, String name, String prefix) {
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        String key = prefix + email;
        redisTemplate.opsForValue().set(key, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

        if (prefix.equals(OTP_SIGNUP_PREFIX)) {
            emailService.sendVerificationEmail(email, name, otp);
        } else {
            emailService.sendPasswordResetEmail(email, name, otp);
        }
        log.info("OTP sent to email: {} with prefix: {}", email, prefix);
    }
}
