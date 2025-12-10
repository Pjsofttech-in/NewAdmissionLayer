package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionCourse;
import com.newadmission.Repository.AdmissionCourseRepository;
import com.newadmission.Service.AdmissionCourseService;
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

@Service
public class AdmissionCourseServiceImpl implements AdmissionCourseService {

    private final AdmissionCourseRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionCourseServiceImpl(AdmissionCourseRepository repository,
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
//    @CacheEvict(value = {"allCourses", "courseById"}, allEntries = true)
    public AdmissionCourse createCourse(AdmissionCourse course, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create course");
        }

        String branchCode = fetchBranchCodeByRole(role, email);
        course.setRole(role);
        course.setCreatedByEmail(email);
        course.setBranchCode(branchCode);
        return repository.save(course);
    }

    @Override
//    @Cacheable(value = "allCourses", key = "#branchCode + '-' + #role + '-' + #email", unless = "#result == null || #result.isEmpty()")
    public List<AdmissionCourse> getAllCourses(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view courses");
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
//    @CacheEvict(value = {"allCourses", "courseById"}, allEntries = true)
    public AdmissionCourse updateCourse(Long id, AdmissionCourse course, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update course");
        }

        AdmissionCourse existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        existing.setCoursename(course.getCoursename());
        return repository.save(existing);
    }

    @Override
//    @CacheEvict(value = {"allCourses", "courseById"}, allEntries = true)
    public void deleteCourse(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete course");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        repository.deleteById(id);
    }

    @Override
//    @Cacheable(value = "courseById", key = "#id + '-' + #role + '-' + #email", unless = "#result == null")
    public AdmissionCourse getCourseById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view course");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }
}