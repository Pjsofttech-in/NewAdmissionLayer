package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionSubject;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.Repository.AdmissionTeacherRepository;
import com.newadmission.Repository.AdmissionSubjectRepository;
import com.newadmission.Service.AdmissionSubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdmissionSubjectServiceImpl implements AdmissionSubjectService {

    @Autowired private AdmissionSubjectRepository repository;
    @Autowired private AdmissionTeacherRepository teacherRepository;
    @Autowired
    private StaffService staffService;
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
                .uri(uriBuilder -> uriBuilder.path(endpoint).queryParam("email", email).build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Set<AdmissionTeacher> fetchTeachersByIds(Set<Integer> ids) {
        return ids.stream()
                .map(id -> teacherRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + id)))
                .collect(Collectors.toSet());
    }

    @Override
//    @CacheEvict(value = {"allSubjects", "subjectById"}, allEntries = true)
    public AdmissionSubject createSubject(AdmissionSubject subject, String role, String email) {
        if (!hasPermission(role, email, "POST")) throw new AccessDeniedException("No permission to create subject");

        String branchCode = fetchBranchCodeByRole(role, email);
        subject.setCreatedByEmail(email);
        subject.setRole(role);
        subject.setBranchCode(branchCode);

        return repository.save(subject);
    }

    @Override
//    @Cacheable(value = "allSubjects", key = "#branchCode + '-' + #role + '-' + #email", unless = "#result == null || #result.isEmpty()")
    public List<AdmissionSubject> getAllSubjects(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) throw new AccessDeniedException("No permission to view subjects");
        return repository.findAllByBranchCode(branchCode);
    }

    @Override
//    @Cacheable(value = "subjectById", key = "#id + '-' + #role + '-' + #email", unless = "#result == null")
    public AdmissionSubject getSubjectById(int id, String role, String email) {
        if (!hasPermission(role, email, "GET")) throw new AccessDeniedException("No permission to view subject");
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Subject not found"));
    }

    @Override
//    @CacheEvict(value = {"allSubjects", "subjectById"}, allEntries = true)
    public AdmissionSubject updateSubject(int id, AdmissionSubject subject, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update subject");
        }

        AdmissionSubject existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        if (subject.getSubjectName() != null) {
            existing.setSubjectName(subject.getSubjectName());
        }



        return repository.save(existing);
    }

    @Override
//    @CacheEvict(value = {"allSubjects", "subjectById"}, allEntries = true)
    public void deleteSubject(int id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) throw new AccessDeniedException("No permission to delete subject");

        repository.findById(id).orElseThrow(() -> new RuntimeException("Subject not found"));
        repository.deleteById(id);
    }

}