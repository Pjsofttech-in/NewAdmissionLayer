package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionTermsAndCondition;
import com.newadmission.Repository.AdmissionTermsAndConditionRepository;
import com.newadmission.Service.AdmissionTermsAndConditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AdmissionTermsAndConditionServiceImpl implements AdmissionTermsAndConditionService {

    private final AdmissionTermsAndConditionRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionTermsAndConditionServiceImpl(AdmissionTermsAndConditionRepository repository,
                                                 WebClient webClient,
                                                 StaffService staffService) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
    }

    private boolean hasPermission(String role, String email, String action) {
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
    public AdmissionTermsAndCondition createTerm(AdmissionTermsAndCondition term, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create terms and conditions");
        }

        String branchCode = fetchBranchCodeByRole(role, email);
        term.setCreatedByEmail(email);
        term.setRole(role);
        term.setBranchCode(branchCode);
        return repository.save(term);
    }

    @Override
    public List<AdmissionTermsAndCondition> getAllTerms(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view terms and conditions");
        }
        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionTermsAndCondition updateTerm(Long id, AdmissionTermsAndCondition term, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update terms and conditions");
        }

        AdmissionTermsAndCondition existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terms and Condition not found"));

        existing.setTermsAndCondition(term.getTermsAndCondition());
        return repository.save(existing);
    }

    @Override
    public void deleteTerm(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete terms and conditions");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terms and Condition not found"));

        repository.deleteById(id);
    }

    @Override
    public AdmissionTermsAndCondition getTermById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view terms and conditions");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Terms and Condition not found"));
    }
}