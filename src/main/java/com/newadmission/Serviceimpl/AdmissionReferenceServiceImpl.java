package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionReference;
import com.newadmission.Repository.AdmissionReferenceRepository;
import com.newadmission.Service.AdmissionReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AdmissionReferenceServiceImpl implements AdmissionReferenceService {

    private final AdmissionReferenceRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionReferenceServiceImpl(AdmissionReferenceRepository repository,
                                         WebClient webClient,
                                         StaffService staffService) {
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
    public AdmissionReference createReference(AdmissionReference reference, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create reference");
        }

        String branchCode = fetchBranchCodeByRole(role, email);
        reference.setRole(role);
        reference.setCreatedByEmail(email);
        reference.setBranchCode(branchCode);
        return repository.save(reference);
    }

    @Override
    public List<AdmissionReference> getAllReferences(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view references");
        }

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionReference updateReference(Long id, AdmissionReference reference, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update reference");
        }

        AdmissionReference existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reference not found"));

        existing.setReference(reference.getReference());
        return repository.save(existing);
    }

    @Override
    public void deleteReference(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete reference");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reference not found"));

        repository.deleteById(id);
    }

    @Override
    public AdmissionReference getReferenceById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view reference");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reference not found"));
    }
}