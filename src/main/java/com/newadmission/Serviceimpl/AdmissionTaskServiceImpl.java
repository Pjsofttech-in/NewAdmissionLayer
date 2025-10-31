package com.newadmission.Serviceimpl;
import com.newadmission.Entity.AdmissionTask;
import com.newadmission.Repository.AdmissionTaskRepository;
import com.newadmission.Service.AdmissionTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;


@Service
public class AdmissionTaskServiceImpl implements AdmissionTaskService {

    private final AdmissionTaskRepository admissionTaskRepository;
    private final WebClient webClient;
    private final StaffService staffService;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionTaskServiceImpl(
            AdmissionTaskRepository admissionTaskRepository,
            WebClient webClient,
            StaffService staffService) {
        this.admissionTaskRepository = admissionTaskRepository;
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
    public AdmissionTask createTask(AdmissionTask task, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to create task");
        }

        String branchCode = fetchBranchCodeByRole(role, email);

        task.setRole(role);
        task.setCreatedByEmail(email);
        task.setBranchCode(branchCode);

        return admissionTaskRepository.save(task);
    }

    @Override
    public List<AdmissionTask> getAllTasks(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view tasks");
        }

        return admissionTaskRepository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionTask getTaskById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view task by ID");
        }

        return admissionTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    @Override
    public AdmissionTask updateTask(Long id, AdmissionTask updated, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to update task");
        }

        AdmissionTask existing = admissionTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        existing.setTaskName(updated.getTaskName());
        existing.setInquiryStatus(updated.getInquiryStatus());
        existing.setRemark(updated.getRemark());
        existing.setDate(updated.getDate());
        existing.setTime(updated.getTime());
        existing.setPriority(updated.getPriority());

        return admissionTaskRepository.save(existing);
    }

    @Override
    public void deleteTask(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("You do not have permission to delete task");
        }

        admissionTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        admissionTaskRepository.deleteById(id);
    }
}