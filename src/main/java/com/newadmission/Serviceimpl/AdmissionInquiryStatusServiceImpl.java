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

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

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

    private boolean hasPermission(String role, String email, String action) {
        if ("USER".equalsIgnoreCase(role)) {
            return "GET".equalsIgnoreCase(action);
        }
        if ("BRANCH".equalsIgnoreCase(role)) {
            try {
                Boolean exists = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/existBranchbyemail")
                                .queryParam("email", email)
                                .build())
                        .retrieve()
                        .bodyToMono(Boolean.class)
                        .block();

                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return switch (role.toUpperCase()) {
            case "STAFF" -> {
                Map<String, Boolean> perms = staffService.getPermissionsByEmail(email);
                yield switch (action.toUpperCase()) {
                    case "GET" -> Boolean.TRUE.equals(perms.get("cansGet"));
                    case "POST" -> Boolean.TRUE.equals(perms.get("cansPost"));
                    case "PUT" -> Boolean.TRUE.equals(perms.get("cansPut"));
                    case "DELETE" -> Boolean.TRUE.equals(perms.get("cansDelete"));
                    default -> false;
                };
            }
            case "DEPARTMENT" -> {
                Map<String, Object> perms = staffService.getCrudPermissionForDepartmentByEmail(email);
                yield switch (action.toUpperCase()) {
                    case "GET" -> Boolean.TRUE.equals(perms.get("candGet"));
                    case "POST" -> Boolean.TRUE.equals(perms.get("candPost"));
                    case "PUT" -> Boolean.TRUE.equals(perms.get("candPut"));
                    case "DELETE" -> Boolean.TRUE.equals(perms.get("candDelete"));
                    default -> false;
                };
            }
            default -> false;
        };
    }

    private String fetchBranchCodeByRole(String role, String email) {
        String endpoint = switch (role.toLowerCase()) {
            case "branch" -> "/branch/getbranchcode";
            case "department" -> "/department/getbranchcode";
            case "staff" -> "/staff/getbranchcode";
            default -> throw new IllegalArgumentException("Invalid role: " + role);
        };

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint)
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public AdmissionInquiryStatus createStatus(AdmissionInquiryStatus status, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to create status");
        }

        String branchCode = fetchBranchCodeByRole(role, email);

        status.setRole(role);
        status.setCreatedByEmail(email);
        status.setBranchCode(branchCode);

        return repository.save(status);
    }

    @Override
    public List<AdmissionInquiryStatus> getAllStatuses(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view statuses");
        }
        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionInquiryStatus getStatusById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view status");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found"));
    }

    @Override
    public AdmissionInquiryStatus updateStatus(Long id, AdmissionInquiryStatus updated, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to update status");
        }

        AdmissionInquiryStatus existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found"));

        existing.setInquiryStatus(updated.getInquiryStatus());

        return repository.save(existing);
    }

    @Override
    public void deleteStatus(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("You do not have permission to delete status");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found with id: " + id));

        repository.deleteById(id);
    }
}