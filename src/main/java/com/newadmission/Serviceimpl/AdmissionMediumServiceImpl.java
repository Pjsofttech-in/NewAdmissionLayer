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

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionMediumServiceImpl(
            AdmissionMediumRepository admissionMediumRepository,
            WebClient webClient,
            StaffService staffService) {
        this.admissionMediumRepository = admissionMediumRepository;
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
    public AdmissionMedium createMedium(AdmissionMedium medium, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to create medium");
        }

        String branchCode = fetchBranchCodeByRole(role, email);

        medium.setRole(role);
        medium.setCreatedByEmail(email);
        medium.setBranchCode(branchCode);

        return admissionMediumRepository.save(medium);
    }

    @Override
    public List<AdmissionMedium> getAllMediums(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
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
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to update medium");
        }

        AdmissionMedium existing = admissionMediumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medium not found"));

        existing.setMediumName(updated.getMediumName());

        return admissionMediumRepository.save(existing);
    }

    @Override
    public void deleteMedium(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("You do not have permission to delete medium");
        }

        admissionMediumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medium not found with id: " + id));

        admissionMediumRepository.deleteById(id);
    }

    @Override
    public AdmissionMedium getMediumById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view medium by ID");
        }

        return admissionMediumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medium not found with id: " + id));
    }
}