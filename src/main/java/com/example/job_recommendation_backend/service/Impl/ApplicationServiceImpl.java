package com.example.job_recommendation_backend.service.Impl;

import com.example.job_recommendation_backend.DTO.*;
import com.example.job_recommendation_backend.entity.Application;
import com.example.job_recommendation_backend.entity.Job;
import com.example.job_recommendation_backend.entity.User;
import com.example.job_recommendation_backend.enums.ApplicationStatus;
import com.example.job_recommendation_backend.enums.Role;
import com.example.job_recommendation_backend.repository.ApplicationRepository;
import com.example.job_recommendation_backend.repository.JobRepository;
import com.example.job_recommendation_backend.repository.UserRepository;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.ApplicationService;
import com.example.job_recommendation_backend.service.EmailService;
import com.example.job_recommendation_backend.service.GoogleCalendarService;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GoogleCalendarService googleCalendarService;


    public long calculateFitScore(CalculateFitScoreDto req) {
        Job job = jobRepository.findById(req.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Set<String> requiredSkills = (job.getRequiredSkills() != null)
                ? job.getRequiredSkills().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet())
                : new HashSet<>();

        if (requiredSkills.isEmpty()) return 0;

        Set<String> resumeSkills = (req.getResumeSkills() != null)
                ? req.getResumeSkills().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet())
                : new HashSet<>();

        resumeSkills.retainAll(requiredSkills);

        return Math.round(((double) resumeSkills.size() / requiredSkills.size()) * 100);
    }

    public ApplicationResponseDto createApplication(CreateApplicationRequestDto request){

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(user.getResumePath() == null || user.getResumePath().isBlank()){
            throw new RuntimeException("Please upload your resume first");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        long fitScore = calculateFitScore(
                CalculateFitScoreDto.builder()
                        .jobId(request.getJobId())
                        .resumeSkills(request.getResumeSkills())
                        .build()
        );

        Application application = Application.builder()
                .user(user)
                .job(job)
                .fitScore(String.valueOf(fitScore))
                .resumePath(user.getResumePath())
                .status(ApplicationStatus.pending)
                .build();

        if(applicationRepository.existsByUserIdAndJobIdAndDeletedAtIsNull(user.getId(), job.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already applied");
        }

        try {
            applicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already applied");
        }

        return mapToResponse(application,null);
    }

    public List<ApplicationResponseDto> allApplications(UUID userId, Role role, Pageable pageable){
        if(role == Role.recruiter)
            return getAllApplicationsForRecruiter(userId, pageable);
        if(role == Role.student)
            return getAllApplicationsForStudent(userId, pageable);
        throw new RuntimeException("Invalid role");
    }

    public ApplicationResponseDto checkApplication(UUID userId, UUID jobId){

        if (jobId == null) {
            throw new RuntimeException("jobId is required");
        }

        return applicationRepository
                .findByUser_IdAndJob_Id(userId, jobId)
                .map(app -> mapToResponse(app, null))
                .orElse(null);
    }

    public String deleteApplication(UUID applicationId, Role role, UUID userId){
        if(role != Role.recruiter){
            throw new RuntimeException("Only recruiter can delete");
        }
        int updated = applicationRepository.softDeleteByRecruiter(applicationId, userId);
        if(updated == 0){
            throw new RuntimeException("Not allowed or application not found");
        }
        return "Application deleted successfully";
    }

    public ApplicationResponseDto updateApplicationStatus(UUID applicationId, UpdateStatusRequestDto request){

        ApplicationStatus status = request.getStatus();
        String notes = request.getNotes();

        if (status == ApplicationStatus.approved) {
            status = ApplicationStatus.interview_scheduled;
        }

        if (!List.of(ApplicationStatus.pending, ApplicationStatus.interview_scheduled, ApplicationStatus.hired,
                ApplicationStatus.rejected).contains(status)) {

            throw new RuntimeException("Invalid status");
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        application.setStatus(status);
        if (notes != null) {
            application.setNotes(notes);
        }
        applicationRepository.save(application);
        return mapToResponse(application, Role.recruiter);
    }

    private List<ApplicationResponseDto> getAllApplicationsForRecruiter(UUID userId,Pageable pageable){
        Page<Application> applications=applicationRepository.findApplicationsForRecruiter(userId, pageable);
        List<ApplicationResponseDto> response= applications.stream()
                .map(app -> mapToResponse(app, Role.recruiter))
                .toList();
        return response;
    }

    private List<ApplicationResponseDto> getAllApplicationsForStudent(UUID userId, Pageable pageable){
        return applicationRepository
                .findApplicationsForStudent(userId, pageable)
                .map(app -> mapToResponse(app, Role.student))
                .getContent();
    }

    private ApplicationResponseDto mapToResponse(Application application, Role role) {

        ApplicationStatus status;
        if (role == Role.student) {
            status = application.getStatus() == ApplicationStatus.interview_scheduled
                    ? ApplicationStatus.approved
                    : application.getStatus();
        } else {
            status = application.getStatus();
        }
        return ApplicationResponseDto.builder()
                .id(application.getId())
                .studentName(
                        application.getUser() != null ? application.getUser().getName() : null
                )
                .companyName(
                        application.getJob() != null ? application.getJob().getCompanyName() : null
                )
                .jobTitle(
                        application.getJob() != null ? application.getJob().getTitle() : null
                )
                .status(status)
                .fitScore(application.getFitScore())
                .createdAt(application.getCreatedAt())
                .build();
    }


    public Resource downloadResume(UUID applicationId){

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));


        UUID currentUserId = UserContext.get().getUserId();
        Role role = UserContext.get().getRole();

        boolean isOwner = application.getUser().getId().equals(currentUserId);
        boolean isRecruiter = application.getJob().getUser().getId().equals(currentUserId);

        if (!(isOwner || isRecruiter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to download this resume");
        }

        String resumeFilename = application.getResumePath();

        if (resumeFilename == null || resumeFilename.isBlank()) {
            throw new RuntimeException("Resume not available");
        }

        Path uploadDir = Paths.get("uploads", "resumes").toAbsolutePath().normalize();
        Path filePath = uploadDir.resolve(resumeFilename).normalize();


        if (!filePath.startsWith(uploadDir)) {
            throw new RuntimeException("Invalid file path");
        }

        if (!Files.exists(filePath)) {
            throw new RuntimeException("Resume file not found on server");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not readable");
            }

            String ext = getFileExtension(resumeFilename);

            String cleanedName = application.getUser().getName()
                    .replaceAll("\\s+", "_") + "_Resume" + ext;

            return new RenamedResource(resource, cleanedName);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Error while reading file", e);
        }
    }

    public class RenamedResource extends InputStreamResource {

        private final String filename;

        public RenamedResource(Resource resource, String filename) {
            super(getSafeStream(resource));
            this.filename = filename;
        }

        private static InputStream getSafeStream(Resource resource) {
            try {
                return resource.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException("Error reading file", e);
            }
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        return (index != -1) ? fileName.substring(index) : "";
    }


    public ApplicationResponseDto scheduleInterview(UUID appId, LocalDateTime interviewDate, String accessToken, UUID currentUserId) {

        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // 🔐 Authorization check
        if (!app.getJob().getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Not authorized");
        }

        // 🔥 Generate Meet link
        String meetLink = googleCalendarService
                .createMeetLink(accessToken, interviewDate);

        app.setInterviewDate(interviewDate);
        app.setInterviewLink(meetLink);
        app.setStatus(ApplicationStatus.interview_scheduled);

        applicationRepository.save(app);

        // 📧 Send email
        try {
            emailService.sendInterviewEmail(
                    app.getUser().getEmail(),
                    app.getUser().getName(),
                    app.getJob().getTitle(),
                    interviewDate,
                    meetLink
            );
        } catch (Exception e) {
            System.out.println("Email failed but interview scheduled");
        }

        return mapToResponse(app, Role.recruiter);
    }
}
