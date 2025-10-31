package com.newadmission.Controller;

import com.newadmission.DTO.BulkEmailRequest;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Repository.AdmissionRepository;
import com.newadmission.Service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class EmailController {

    private final EmailService emailService;
    private final AdmissionRepository admissionRepository;

    @PostMapping("/sendBulkEmails")
    public String sendBulkEmails(@RequestBody BulkEmailRequest request) {
        List<AdmissionForm> admissions = admissionRepository.findAllById(request.getIds());

        ExecutorService executor = Executors.newFixedThreadPool(10); // You can adjust the pool size

        for (AdmissionForm admission : admissions) {
            if (admission.getEmail() != null && !admission.getEmail().isBlank()) {
                executor.submit(() -> {
                    emailService.sendCustomEmail(admission.getEmail(), request.getSubject(), request.getMessage());
                });
            }
        }

        executor.shutdown(); // Initiates an orderly shutdown

        return "✅ Email dispatch initiated to " + admissions.size() + " admissions.";
    }
}