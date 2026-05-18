package com.example.job_recommendation_backend.controller;

import com.example.job_recommendation_backend.entity.ContactMessage;
import com.example.job_recommendation_backend.repository.ContactMessageRepository;
import com.example.job_recommendation_backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private ContactMessageRepository contactMessageRepository;
    
    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<String> submitContactMessage(@RequestBody ContactMessage message) {
        contactMessageRepository.save(message);
        emailService.sendContactUsEmail(message.getName(), message.getEmail(), message.getSubject(), message.getMessage());
        return ResponseEntity.ok("Message received successfully");
    }
}
