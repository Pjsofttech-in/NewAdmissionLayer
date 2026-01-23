package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionInquiryStatus;
import com.newadmission.Repository.AdmissionInquiryStatusRepository;
import com.newadmission.Service.AdmissionInquiryStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;


@Service
public class AdmissionInquiryStatusServiceImpl implements AdmissionInquiryStatusService {

    private final AdmissionInquiryStatusRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;

    @Autowired
    public AdmissionInquiryStatusServiceImpl(
            AdmissionInquiryStatusRepository repository,
            WebClient webClient,
            StaffService staffService
    ) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
    }

    @Override
    public AdmissionInquiryStatus createStatus(AdmissionInquiryStatus status, String role, String email) {
        if (!staffService.hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to create status");
        }

        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        status.setRole(role);
        status.setCreatedByEmail(email);
        status.setBranchCode(branchCode);

        return repository.save(status);
    }

    @Override
    public List<AdmissionInquiryStatus> getAllStatuses(String role, String email, String branchCode) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view statuses");
        }
        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionInquiryStatus getStatusById(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view status");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found"));
    }

    @Override
    public AdmissionInquiryStatus updateStatus(Long id, AdmissionInquiryStatus updated, String role, String email) {
        if (!staffService.hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to update status");
        }

        AdmissionInquiryStatus existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found"));

        existing.setInquiryStatus(updated.getInquiryStatus());

        return repository.save(existing);
    }

    @Override
    public void deleteStatus(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("You do not have permission to delete status");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found with id: " + id));

        repository.deleteById(id);
    }
}