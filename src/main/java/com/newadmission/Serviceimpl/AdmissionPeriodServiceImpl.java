package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionPeriod;
import com.newadmission.Entity.AdmissionTimetable;
import com.newadmission.Repository.AdmissionPeriodRepository;
import com.newadmission.Repository.AdmissionTimetableRepository;
import com.newadmission.Service.AdmissionPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class AdmissionPeriodServiceImpl implements AdmissionPeriodService {

    @Autowired
    private AdmissionPeriodRepository periodRepository;
    @Autowired private AdmissionTimetableRepository timetableRepository;
    @Autowired private StaffService staffService;
    @Autowired private WebClient webClient;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    private boolean hasPermission(String role, String email, String action) {
        if ("BRANCH".equalsIgnoreCase(role)) {
            try {
                Boolean exists = webClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/existBranchbyemail")
                                .queryParam("email", email).build())
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
                var perms = staffService.getPermissionsByEmail(email);
                yield switch (action.toUpperCase()) {
                    case "GET" -> Boolean.TRUE.equals(perms.get("cansGet"));
                    case "POST" -> Boolean.TRUE.equals(perms.get("cansPost"));
                    case "PUT" -> Boolean.TRUE.equals(perms.get("cansPut"));
                    case "DELETE" -> Boolean.TRUE.equals(perms.get("cansDelete"));
                    default -> false;
                };
            }
            case "DEPARTMENT" -> {
                var perms = staffService.getCrudPermissionForDepartmentByEmail(email);
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
                .uri(uriBuilder -> uriBuilder.path(endpoint).queryParam("email", email).build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public AdmissionPeriod createPeriod(AdmissionPeriod period, String role, String email) {
        if (!hasPermission(role, email, "POST"))
            throw new AccessDeniedException("You don't have permission to add period");

        period.setCreatedByEmail(email);
        period.setRole(role);
        period.setBranchCode(fetchBranchCodeByRole(role, email));

        return periodRepository.save(period);
    }

    @Override
    public AdmissionPeriod updatePeriod(Integer id, AdmissionPeriod updated, String role, String email) {
        if (!hasPermission(role, email, "PUT"))
            throw new AccessDeniedException("You don't have permission to update period");

        AdmissionPeriod existing = periodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        // 🔁 Partial update (don’t overwrite nulls)
        if (updated.getPeriodNo() != null) existing.setPeriodNo(updated.getPeriodNo());
        if (updated.getStartTime() != null) existing.setStartTime(updated.getStartTime());
        if (updated.getEndTime() != null) existing.setEndTime(updated.getEndTime());

        return periodRepository.save(existing);
    }

    @Override
    public List<AdmissionPeriod> getAllPeriods(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET"))
            throw new AccessDeniedException("You don't have permission to view periods");

        return periodRepository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionPeriod getPeriodById(Integer id, String role, String email) {
        if (!hasPermission(role, email, "GET"))
            throw new AccessDeniedException("You don't have permission to view period");

        return periodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Period not found"));
    }

    @Override
    public void deletePeriod(Integer id, String role, String email) {
        if (!hasPermission(role, email, "DELETE"))
            throw new AccessDeniedException("You don't have permission to delete period");

        AdmissionPeriod period = periodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        periodRepository.delete(period);
    }
}