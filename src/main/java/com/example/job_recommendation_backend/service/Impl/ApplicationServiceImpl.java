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
import com.example.job_recommendation_backend.repository.projection.StudentAnalytics;
import com.example.job_recommendation_backend.security.UserContext;
import com.example.job_recommendation_backend.service.ApplicationService;
import com.example.job_recommendation_backend.service.EmailService;
import com.example.job_recommendation_backend.service.GoogleCalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.job_recommendation_backend.exception.CustomApiException;
import com.example.job_recommendation_backend.exception.ResourceNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final GoogleCalendarService googleCalendarService;

    public ApplicationServiceImpl(
            ApplicationRepository applicationRepository,
            JobRepository jobRepository,
            UserRepository userRepository,
            EmailService emailService,
            GoogleCalendarService googleCalendarService) {

        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.googleCalendarService = googleCalendarService;
    }


    @Override
    public long calculateFitScore(CalculateFitScoreDto req) {
        Job job = jobRepository.findById(req.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", req.getJobId().toString()));

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

        Set<String> commonSkills = new HashSet<>(resumeSkills);
        commonSkills.retainAll(requiredSkills);

        return Math.round(((double) commonSkills.size() / requiredSkills.size()) * 100);
    }

    public StudentAnalytics getStudentAnalytics(UUID userId){
        return  applicationRepository.getStudentAnalytics(userId);
    }
    @Override
    public Map<UUID, Long> calculateFitScores(CalculateFitScoreBatchRequestDto req) {
        List<Job> jobs = jobRepository.findAllById(req.getJobIds());
        Map<UUID, Long> scores = new HashMap<>();

        Set<String> resumeSkills = (req.getResumeSkills() != null)
                ? req.getResumeSkills().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet())
                : new HashSet<>();

        for (Job job : jobs) {
            Set<String> requiredSkills = (job.getRequiredSkills() != null)
                    ? job.getRequiredSkills().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet())
                    : new HashSet<>();

            if (requiredSkills.isEmpty()) {
                scores.put(job.getId(), 0L);
                continue;
            }

            Set<String> commonSkills = new HashSet<>(resumeSkills);
            commonSkills.retainAll(requiredSkills);

            long score = Math.round(((double) commonSkills.size() / requiredSkills.size()) * 100);
            scores.put(job.getId(), score);
        }
        return scores;
    }

    public ApplicationResponseDto createApplication(CreateApplicationRequestDto request){

        var context = UserContext.get();
        UUID userId = context.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        if(user.getResumePath() == null || user.getResumePath().isBlank()){
            throw new CustomApiException("Please upload your resume first");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", request.getJobId().toString()));

        long fitScore = calculateFitScore(
                CalculateFitScoreDto.builder()
                        .jobId(request.getJobId())
                        .resumeSkills(user.getSkills())
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
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Already applied");
        }

        try {
            applicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Already applied");
        }

        return mapToResponse(application,null);
    }

    public Page<ApplicationResponseDto> allApplications(UUID userId, Role role, Pageable pageable){
        if(role == Role.recruiter)
            return getAllApplicationsForRecruiter(userId, pageable);
        if(role == Role.student)
            return getAllApplicationsForStudent(userId, pageable);
        if(role == Role.admin){
            return applicationRepository.findAllApplications(pageable);
        }
        throw new CustomApiException("Invalid role");
    }

    public ApplicationResponseDto checkApplication(UUID userId, UUID jobId){

        if (jobId == null) {
            throw new CustomApiException("jobId is required");
        }

        return applicationRepository
                .findByUser_IdAndJob_IdAndDeletedAtIsNull(userId, jobId)
                .map(app -> mapToResponse(app, null))
                .orElse(null);
    }

    public String deleteApplication(UUID applicationId, Role role, UUID userId){
        if(role == Role.student){
            throw new CustomApiException(HttpStatus.FORBIDDEN, "You are not allowed to do this action");
        }
        int updated =0;
        if(role == Role.admin){
            updated= applicationRepository.softDeleteByAdmin(applicationId);
        }else{
            updated = applicationRepository.softDeleteByRecruiter(applicationId, userId);
        }
        if(updated == 0){
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Not allowed or application not found");
        }
        return "Application deleted successfully";
    }

    public ApplicationResponseDto updateApplicationStatus(UUID applicationId, UpdateStatusRequestDto request){

        // Todo: have to No ownership validation who can update any application
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId.toString()));

        UUID currentUserId = UserContext.get().getUserId();

        if (!application.getJob().getUser().getId().equals(currentUserId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized to update this application");
        }

        ApplicationStatus status = request.getStatus();
        String notes = request.getNotes();

        if (status == ApplicationStatus.approved) {
            status = ApplicationStatus.interview_scheduled;
        }

        if (!List.of(ApplicationStatus.pending, ApplicationStatus.interview_scheduled, ApplicationStatus.hired,
                ApplicationStatus.rejected).contains(status)) {

            throw new CustomApiException("Invalid status");
        }
        application.setStatus(status);
        if (notes != null) {
            application.setNotes(notes);
        }
        applicationRepository.save(application);
        return mapToResponse(application, Role.recruiter);
    }

    private Page<ApplicationResponseDto> getAllApplicationsForRecruiter(UUID userId,Pageable pageable){
        Page<Application> applications=applicationRepository.findActiveApplicationsForRecruiter(userId, pageable);
        return applications.map(app -> mapToResponse(app, Role.recruiter));
    }

    private Page<ApplicationResponseDto> getAllApplicationsForStudent(UUID userId, Pageable pageable){
        return applicationRepository
                .findActiveApplicationsForStudent(userId, pageable)
                .map(app -> mapToResponse(app, Role.student));
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
                .fitScore(application.getFitScore() != null ? application.getFitScore().toString() : null)
                .createdAt(application.getCreatedAt())
                .recruiterName(application.getJob() != null && application.getJob().getUser() != null ? application.getJob().getUser().getName() : null)
                .job(mapJobToResponseDto(application.getJob()))
                .build();
    }

    private JobResponseDto mapJobToResponseDto(Job job) {
        if (job == null) return null;

        RecruiterDto recruiter = job.getUser() == null ? null :
                RecruiterDto.builder()
                        .name(job.getUser().getName())
                        .email(job.getUser().getEmail())
                        .build();

        return JobResponseDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .location(job.getLocation())
                .salary(job.getSalary())
                .type(job.getType())
                .experience(job.getExperience())
                .remote(job.getRemote())
                .isActive(job.getIsActive())
                .companyName(job.getCompanyName())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .recruiter(recruiter)
                .build();
    }

    public Resource downloadResume(UUID applicationId){

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId.toString()));

        UUID currentUserId = UserContext.get().getUserId();

        boolean isOwner = application.getUser().getId().equals(currentUserId);
        boolean isRecruiter = application.getJob().getUser().getId().equals(currentUserId);

        if (!(isOwner || isRecruiter)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "You are not allowed to download this resume");
        }

        Path uploadDir = Paths.get("uploads", "resumes").toAbsolutePath().normalize();
        
        // 1. Try application-specific resume path
        String resumeFilename = application.getResumePath();
        Path filePath = (resumeFilename != null && !resumeFilename.isBlank()) 
                ? uploadDir.resolve(resumeFilename).normalize() 
                : null;

        // 2. Fallback to user's current resume if original is missing or file doesn't exist
        if (filePath == null || !Files.exists(filePath)) {
            log.info("Original resume file {} missing, falling back to student's current resume", resumeFilename);
            resumeFilename = application.getUser().getResumePath();
            filePath = (resumeFilename != null && !resumeFilename.isBlank()) 
                    ? uploadDir.resolve(resumeFilename).normalize() 
                    : null;
        }

        if (filePath == null || !Files.exists(filePath)) {
            throw new CustomApiException(HttpStatus.NOT_FOUND, "Resume file not found on server");
        }

        if (!filePath.startsWith(uploadDir)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "File not readable");
            }

            String ext = getFileExtension(resumeFilename);
            String cleanedName = (application.getUser() != null ? application.getUser().getName() : "Candidate")
                    .replaceAll("[^a-zA-Z0-9.-]", "_") + "_Resume" + ext;

            return new RenamedResource(resource, cleanedName);

        } catch (IOException e) {
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while reading file");
        }
    }

    public static class RenamedResource extends UrlResource {
        private final String filename;
        private final long length;

        public RenamedResource(Resource resource, String filename) throws IOException {
            super(resource.getURI());
            this.filename = filename;
            long len = -1;
            try {
                len = resource.contentLength();
            } catch (IOException ignored) {}
            this.length = len;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public long contentLength() throws IOException {
            return length != -1 ? length : super.contentLength();
        }
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        return (index != -1) ? fileName.substring(index) : "";
    }

    public ApplicationResponseDto scheduleInterview(UUID appId, LocalDateTime interviewDate) {

        UUID currentUserId = UserContext.get().getUserId();

        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", appId.toString()));

        if (!app.getJob().getUser().getId().equals(currentUserId)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        String meetLink;
        try {
            meetLink = googleCalendarService.createMeetLink(interviewDate, app.getUser().getEmail());
        } catch (Exception e) {
            log.error("Failed to create meet link", e);
            throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to schedule interview");
        }

        app.setInterviewDate(interviewDate);
        app.setInterviewLink(meetLink);
        app.setStatus(ApplicationStatus.interview_scheduled);

        applicationRepository.save(app);

        emailService.sendInterviewEmail(
                app.getUser().getEmail(),
                app.getUser().getName(),
                app.getJob().getTitle(),
                interviewDate,
                meetLink
        );

        return mapToResponse(app, Role.recruiter);
    }
}
