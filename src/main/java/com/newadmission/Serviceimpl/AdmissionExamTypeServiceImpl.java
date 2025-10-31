package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionExamType;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.Repository.AdmissionExamTypeRepository;
import com.newadmission.Repository.AdmissionTeacherRepository;
import com.newadmission.Service.AdmissionExamTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AdmissionExamTypeServiceImpl implements AdmissionExamTypeService {

    private final AdmissionExamTypeRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;
    private final AdmissionTeacherRepository admissionTeacherRepository;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionExamTypeServiceImpl(AdmissionExamTypeRepository repository,
                                        WebClient webClient,
                                        StaffService staffService,
                                        AdmissionTeacherRepository admissionTeacherRepository) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
        this.admissionTeacherRepository =admissionTeacherRepository;
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
    public AdmissionExamType createExamType(AdmissionExamType examType, String role, String email) {
        if (!hasPermission(role, email, "POST"))
            throw new AccessDeniedException("No permission to create ExamType");

        String branchCode = fetchBranchCodeByRole(role, email);
        examType.setCreatedByEmail(email);
        examType.setRole(role);
        examType.setBranchCode(branchCode);

        return repository.save(examType);
    }

    @Override
    public List<AdmissionExamType> getAllExamTypes(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET"))
            throw new AccessDeniedException("No permission to view ExamType list");

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionExamType getExamTypeById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET"))
            throw new AccessDeniedException("No permission to view ExamType");

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ExamType not found"));
    }

    @Override
    public AdmissionExamType updateExamType(Long id, AdmissionExamType examType, String role, String email) {
        if (!hasPermission(role, email, "PUT"))
            throw new AccessDeniedException("No permission to update ExamType");

        AdmissionExamType existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ExamType not found"));

        existing.setExamType(examType.getExamType());
        return repository.save(existing);
    }

    @Override
    public void deleteExamType(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE"))
            throw new AccessDeniedException("No permission to delete ExamType");

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ExamType not found"));

        repository.deleteById(id);
    }

    @Override
    public List<AdmissionExamType> getExamTypesByTeacherEmailAndBranchCode(String teacherEmail, String branchCode) {
        AdmissionTeacher teacher = admissionTeacherRepository.findByEmailAndBranchCode(teacherEmail, branchCode)
                .orElseThrow(() -> new RuntimeException("Teacher not found with provided email and branch code"));

        return repository.findAllByBranchCode(branchCode);
    }

}