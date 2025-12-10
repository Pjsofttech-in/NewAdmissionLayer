package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionSourceBy;
import com.newadmission.Repository.AdmissionSourceByRepository;
import com.newadmission.Service.AdmissionSourceByService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdmissionSourceByServiceImpl implements AdmissionSourceByService {

    private final AdmissionSourceByRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionSourceByServiceImpl(
            AdmissionSourceByRepository repository,
            WebClient webClient,
            StaffService staffService) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
    }

    private boolean hasPermission(String role, String email, String action) {

        if ("SUPERADMIN".equalsIgnoreCase(role) && "GET".equalsIgnoreCase(action)) {
            try {
                Boolean exists = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/checkClientEmailExist")
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
        if ("USER".equalsIgnoreCase(role)) {
            return "POST".equalsIgnoreCase(action) || "GET".equalsIgnoreCase(action);
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
//    @CacheEvict(value = {"allSourceBy", "sourceById"}, allEntries = true)
    public AdmissionSourceBy createSourceBy(AdmissionSourceBy sourceBy, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to create source by");
        }

        String branchCode = fetchBranchCodeByRole(role, email);
        sourceBy.setBranchCode(branchCode);
        sourceBy.setCreatedByEmail(email);
        sourceBy.setRole(role);

        return repository.save(sourceBy);
    }

    @Override
//    @Cacheable(value = "allSourceBy", key = "#branchCode + '-' + #role + '-' + #email", unless = "#result == null || #result.isEmpty()")
    public List<AdmissionSourceBy> getAllSourceBy(String role, String email, String branchCode) {

        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view sources");
        }

        try {
            if ("SUPERADMIN".equalsIgnoreCase(role)) {
                if (branchCode != null && !branchCode.trim().isEmpty()) {
                    return repository.findAllByBranchCode(branchCode);
                }

                List<String> branchCodes = staffService.getBranchCodesByInstituteEmail(email);
                if (branchCodes == null || branchCodes.isEmpty()) {
                    return Collections.emptyList();
                }
                return repository.findAllByBranchCodeIn(branchCodes);
            }

            return repository.findAllByBranchCode(branchCode);

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }



    @Override
//    @CacheEvict(value = {"allSourceBy", "sourceById"}, allEntries = true)
    public AdmissionSourceBy updateSourceBy(Long id, AdmissionSourceBy updated, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to update source by");
        }

        AdmissionSourceBy existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Source by not found"));

        existing.setSourceBy(updated.getSourceBy());
        return repository.save(existing);
    }

    @Override
//    @CacheEvict(value = {"allSourceBy", "sourceById"}, allEntries = true)
    public void deleteSourceBy(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("You do not have permission to delete source by");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Source by not found"));

        repository.deleteById(id);
    }

    @Override
//    @Cacheable(value = "sourceById", key = "#id + '-' + #role + '-' + #email", unless = "#result == null")
    public AdmissionSourceBy getSourceById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view source by ID");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Source by not found"));
    }
}