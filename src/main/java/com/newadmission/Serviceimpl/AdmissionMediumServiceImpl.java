package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionMedium;
import com.newadmission.Repository.AdmissionMediumRepository;
import com.newadmission.Service.AdmissionMediumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdmissionMediumServiceImpl implements AdmissionMediumService {

    private final AdmissionMediumRepository admissionMediumRepository;
    private final WebClient webClient;
    private final StaffService staffService;
    
    @Autowired
    public AdmissionMediumServiceImpl(
            AdmissionMediumRepository admissionMediumRepository,
            WebClient webClient,
            StaffService staffService) {
        this.admissionMediumRepository = admissionMediumRepository;
        this.webClient = webClient;
        this.staffService = staffService;
    }

 
    @Override
    public AdmissionMedium createMedium(AdmissionMedium medium, String role, String email) {
        if (!staffService.hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to create medium");
        }

        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        medium.setRole(role);
        medium.setCreatedByEmail(email);
        medium.setBranchCode(branchCode);

        return admissionMediumRepository.save(medium);
    }

    @Override
    public List<AdmissionMedium> getAllMediums(String role, String email, String branchCode) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view mediums");
        }

        try {
            if ("SUPERADMIN".equalsIgnoreCase(role)) {
                if (branchCode != null && !branchCode.trim().isEmpty()) {
                    return admissionMediumRepository.findAllByBranchCode(branchCode);
                }

                List<String> branchCodes = staffService.getBranchCodesByInstituteEmail(email);
                if (branchCodes == null || branchCodes.isEmpty()) {
                    return Collections.emptyList();
                }
                return admissionMediumRepository.findAllByBranchCodeIn(branchCodes);
            }

            return admissionMediumRepository.findAllByBranchCode(branchCode);

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public AdmissionMedium updateMedium(Long id, AdmissionMedium updated, String role, String email) {
        if (!staffService.hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to update medium");
        }

        AdmissionMedium existing = admissionMediumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medium not found"));

        existing.setMediumName(updated.getMediumName());

        return admissionMediumRepository.save(existing);
    }

    @Override
    public void deleteMedium(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("You do not have permission to delete medium");
        }

        admissionMediumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medium not found with id: " + id));

        admissionMediumRepository.deleteById(id);
    }

    @Override
    public AdmissionMedium getMediumById(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view medium by ID");
        }

        return admissionMediumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medium not found with id: " + id));
    }
}