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

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionConductByServiceImpl(AdmissionConductByRepository repository,
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
    public AdmissionConductBy createConductBy(AdmissionConductBy conductBy, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create ConductBy");
        }

        String branchCode = fetchBranchCodeByRole(role, email);
        conductBy.setRole(role);
        conductBy.setCreatedByEmail(email);
        conductBy.setBranchCode(branchCode);
        return repository.save(conductBy);
    }

    @Override
    public List<AdmissionConductBy> getAllConductBy(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view ConductBy list");
        }

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionConductBy updateConductBy(Long id, AdmissionConductBy conductBy, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update ConductBy");
        }

        AdmissionConductBy existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ConductBy not found"));

        existing.setGuideName(conductBy.getGuideName());
        return repository.save(existing);
    }

    @Override
    public void deleteConductBy(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete ConductBy");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ConductBy not found"));

        repository.deleteById(id);
    }

    @Override
    public AdmissionConductBy getConductById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view ConductBy");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ConductBy not found"));
    }
}