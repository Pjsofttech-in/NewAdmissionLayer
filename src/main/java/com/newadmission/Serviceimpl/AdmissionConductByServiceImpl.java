package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionConductBy;
import com.newadmission.Repository.AdmissionConductByRepository;
import com.newadmission.Service.AdmissionConductByService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AdmissionConductByServiceImpl implements AdmissionConductByService {

    private final AdmissionConductByRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;

    @Autowired
    public AdmissionConductByServiceImpl(AdmissionConductByRepository repository,
                                         WebClient webClient,
                                         StaffService staffService) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
    }

    @Override
    public AdmissionConductBy createConductBy(AdmissionConductBy conductBy, String role, String email) {
        if (!staffService.hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create ConductBy");
        }

        String branchCode = staffService.fetchBranchCodeByRole(role, email);
        conductBy.setRole(role);
        conductBy.setCreatedByEmail(email);
        conductBy.setBranchCode(branchCode);
        return repository.save(conductBy);
    }

    @Override
    public List<AdmissionConductBy> getAllConductBy(String role, String email, String branchCode) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view ConductBy list");
        }

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionConductBy updateConductBy(Long id, AdmissionConductBy conductBy, String role, String email) {
        if (!staffService.hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update ConductBy");
        }

        AdmissionConductBy existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ConductBy not found"));

        existing.setGuideName(conductBy.getGuideName());
        return repository.save(existing);
    }

    @Override
    public void deleteConductBy(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete ConductBy");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ConductBy not found"));

        repository.deleteById(id);
    }

    @Override
    public AdmissionConductBy getConductById(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view ConductBy");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ConductBy not found"));
    }
}