package com.newadmission.Serviceimpl;

import com.newadmission.Entity.*;
import com.newadmission.Repository.*;
import com.newadmission.Service.ClassRoomSubjectDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ClassRoomSubjectDetailsServiceImpl implements ClassRoomSubjectDetailsService {

    @Autowired
    private ClassRoomSubjectDetailsRepository classRoomSubjectDetailsRepository;

    @Autowired
    private AdmissionClassRoomRepository classroomRepo;

    @Autowired
    private AdmissionSubjectRepository subjectRepo;

    @Autowired
    private AdmissionExamTypeRepository examTypeRepo;

    @Autowired
    private AdmissionPaperTypeRepository paperTypeRepo;

    @Autowired
    private  AdmissionTeacherRepository admissionTeacherRepository;

    @Autowired
    private WebClient webClient;

    @Autowired
    private StaffService staffService;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

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
            case "TEACHER" -> {
                boolean exists = admissionTeacherRepository.existsByEmail(email);
                yield exists;
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
    public ClassRoomSubjectDetails create(ClassRoomSubjectDetails details, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create");
        }

        AdmissionClassRoom classroom = classroomRepo.findById(details.getClassroom().getId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        AdmissionSubject subject = subjectRepo.findById(details.getSubject().getId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        AdmissionExamType examType = examTypeRepo.findById(details.getExamType().getId())
                .orElseThrow(() -> new RuntimeException("Exam type not found"));

        AdmissionPaperType paperType = paperTypeRepo.findById(details.getPaperType().getId())
                .orElseThrow(() -> new RuntimeException("Paper type not found"));

        details.setClassroom(classroom);
        details.setSubject(subject);
        details.setExamType(examType);
        details.setPaperType(paperType);

        String branchCode = null;
        if ("teacher".equalsIgnoreCase(role)) {
            branchCode = admissionTeacherRepository.findByEmail(email)
                    .map(AdmissionTeacher::getBranchCode)
                    .orElseThrow(() -> new RuntimeException("Branch code not found for teacher"));
        } else {
            branchCode = fetchBranchCodeByRole(role, email);
        }

        // ✅ Unique check for topicName per branch
        if (classRoomSubjectDetailsRepository.findByTopicNameAndBranchCode(details.getTopicName(), branchCode).isPresent()) {
            throw new RuntimeException("Topic name already exists for this branch.");
        }

        details.setRole(role);
        details.setCreatedByEmail(email);
        details.setBranchCode(branchCode);

        return classRoomSubjectDetailsRepository.save(details);
    }

    @Override
    public List<ClassRoomSubjectDetails> getAll(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view");
        }

        return classRoomSubjectDetailsRepository.findAllByBranchCode(branchCode);
    }

    @Override
//    @Cacheable(value = "classRoomSubjectDetailsById", key = "#id + '-' + #role + '-' + #email")
    public ClassRoomSubjectDetails getById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view");
        }

        return classRoomSubjectDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "classRoomSubjectDetailsById", key = "#id + '-' + #role + '-' + #email"),
//            @CacheEvict(value = "allClassRoomSubjectDetails", allEntries = true),
//            @CacheEvict(value = "topicNamesByFilters", allEntries = true)
//    })
    public ClassRoomSubjectDetails update(Long id, ClassRoomSubjectDetails details, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        if (details == null) {
            throw new IllegalArgumentException("Details cannot be null");
        }

        // Null and validity checks for nested entity references and their IDs:
        if (details.getClassroom() == null || details.getClassroom().getId() == null) {
            throw new IllegalArgumentException("Classroom or Classroom ID cannot be null");
        }
        if (details.getSubject() == null || details.getSubject().getId() <= 0) {
            throw new IllegalArgumentException("Subject or Subject ID is invalid");
        }

        if (details.getExamType() == null || details.getExamType().getId() == null) {
            throw new IllegalArgumentException("ExamType or ExamType ID cannot be null");
        }
        if (details.getPaperType() == null || details.getPaperType().getId() == null) {
            throw new IllegalArgumentException("PaperType or PaperType ID cannot be null");
        }
        if (details.getTopicName() == null || details.getTopicName().isBlank()) {
            throw new IllegalArgumentException("Topic name cannot be null or empty");
        }

        // Fetch the existing entity or throw if not found
        ClassRoomSubjectDetails existing = classRoomSubjectDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ClassRoomSubjectDetails not found"));

        // Fetch referenced entities, throw if not found
        AdmissionClassRoom classroom = classroomRepo.findById(details.getClassroom().getId())
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        AdmissionSubject subject = subjectRepo.findById(details.getSubject().getId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        AdmissionExamType examType = examTypeRepo.findById(details.getExamType().getId())
                .orElseThrow(() -> new RuntimeException("Exam type not found"));

        AdmissionPaperType paperType = paperTypeRepo.findById(details.getPaperType().getId())
                .orElseThrow(() -> new RuntimeException("Paper type not found"));

        // Unique topic name check (only if topic name changed)
        if (!existing.getTopicName().equalsIgnoreCase(details.getTopicName())) {
            boolean exists = classRoomSubjectDetailsRepository
                    .findByTopicNameAndBranchCode(details.getTopicName(), existing.getBranchCode())
                    .isPresent();

            if (exists) {
                throw new RuntimeException("Topic name already exists for this branch.");
            }
        }

        // Update fields
        existing.setTotalMarks(details.getTotalMarks());
        existing.setPassingMarks(details.getPassingMarks());
        existing.setClassroom(classroom);
        existing.setSubject(subject);
        existing.setExamType(examType);
        existing.setPaperType(paperType);
        existing.setTopicName(details.getTopicName());

        // Save and return updated entity
        return classRoomSubjectDetailsRepository.save(existing);
    }


    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "classRoomSubjectDetailsById", key = "#id + '-' + #role + '-' + #email"),
//            @CacheEvict(value = "allClassRoomSubjectDetails", allEntries = true),
//            @CacheEvict(value = "topicNamesByFilters", allEntries = true)
//    })
    public void delete(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete");
        }

        classRoomSubjectDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        classRoomSubjectDetailsRepository.deleteById(id);
    }

    @Override
    public Long getIdByTopicName(String topicName, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to fetch data");
        }

        ClassRoomSubjectDetails details = classRoomSubjectDetailsRepository.findByTopicName(topicName)
                .orElseThrow(() -> new RuntimeException("Topic name not found: " + topicName));

        return details.getId();
    }

    @Override
//    @Cacheable(value = "topicNamesByFilters",
//            key = "#classroomId + '-' + #subjectId + '-' + #examTypeId + '-' + #paperTypeId + '-' + #role + '-' + #email")
    public List<String> getTopicNamesByFilters(Long classroomId, Long subjectId, Long examTypeId, Long paperTypeId, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to fetch topic names");
        }

        String branchCode;
        if ("teacher".equalsIgnoreCase(role)) {
            branchCode = admissionTeacherRepository.findByEmail(email)
                    .map(teacher -> teacher.getBranchCode())
                    .orElseThrow(() -> new RuntimeException("Branch code not found for teacher"));
        } else {
            branchCode = fetchBranchCodeByRole(role, email);
        }

        return classRoomSubjectDetailsRepository.findTopicNamesByAllIdsAndBranchCode(
                classroomId, subjectId, examTypeId, paperTypeId, branchCode);
    }

}