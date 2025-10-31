package com.newadmission.Serviceimpl;

import com.newadmission.DTO.StudentResultResponse;
import com.newadmission.DTO.SubjectResultDto;
import com.newadmission.Entity.*;
import com.newadmission.Repository.*;
import com.newadmission.Service.StudentSubjectResultService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentSubjectResultServiceImpl implements StudentSubjectResultService {

    @Autowired
    private StudentSubjectResultRepository repository;

    @Autowired
    private AdmissionRepository admissionFormRepository;

    @Autowired
    private ClassRoomSubjectDetailsRepository subjectDetailsRepository;

    @Autowired
    private AdmissionExamTypeRepository examTypeRepository;

    @Autowired
    private AdmissionPaperTypeRepository paperTypeRepository;

    @Autowired
    private AdmissionTeacherRepository admissionTeacherRepository;

    @Autowired
    private StudentSubjectResultRepository studentSubjectResultRepository;

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
    public List<StudentSubjectResult> createMultiple(List<StudentSubjectResult> results, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create");
        }

        String branchCode;
        if ("teacher".equalsIgnoreCase(role)) {
            branchCode = admissionTeacherRepository.findByEmail(email)
                    .map(AdmissionTeacher::getBranchCode)
                    .orElseThrow(() -> new RuntimeException("Branch code not found for teacher"));
        } else {
            branchCode = fetchBranchCodeByRole(role, email);
        }

        List<StudentSubjectResult> savedResults = new ArrayList<>();

        // Step 1: Group total obtained marks per student-exam-paper
        Map<String, Integer> totalMarksMap = new HashMap<>();
        for (StudentSubjectResult result : results) {
            String key = result.getStudent().getId() + "-" +
                    result.getExamType().getId() + "-" +
                    result.getPaperType().getId();
            totalMarksMap.put(key, totalMarksMap.getOrDefault(key, 0) + result.getObtainedMarks());
        }

        for (StudentSubjectResult result : results) {
            result.setRole(role);
            result.setCreatedByEmail(email);
            result.setBranchCode(branchCode);

            Long studentId = result.getStudent().getId();
            Long subjectDetailsId = result.getSubjectDetails().getId();
            Long examTypeId = result.getExamType().getId();
            Long paperTypeId = result.getPaperType().getId();

            // ✅ Fetch SubjectDetails by ID
            ClassRoomSubjectDetails subjectDetails = subjectDetailsRepository.findById(subjectDetailsId)
                    .orElseThrow(() -> new RuntimeException("SubjectDetails not found with ID: " + subjectDetailsId));

            // ✅ Fetch associated entities
            AdmissionClassRoom classroom = subjectDetails.getClassroom();
            AdmissionSubject subject = subjectDetails.getSubject();
            AdmissionExamType examType = examTypeRepository.findById(examTypeId)
                    .orElseThrow(() -> new RuntimeException("ExamType not found with ID: " + examTypeId));
            AdmissionPaperType paperType = paperTypeRepository.findById(paperTypeId)
                    .orElseThrow(() -> new RuntimeException("PaperType not found with ID: " + paperTypeId));

            // ✅ Find valid subject-classroom-exam-paper combo
            List<ClassRoomSubjectDetails> validCombos =
                    subjectDetailsRepository.findByClassroomAndSubjectAndExamTypeAndPaperType(
                            classroom, subject, examType, paperType);

            if (validCombos.isEmpty()) {
                throw new RuntimeException("Invalid ExamType or PaperType for the given Classroom and Subject");
            }

            // ✅ Re-fetch student for consistency
            AdmissionForm student = admissionFormRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

            // ✅ Assign all associations
            result.setStudent(student);
            result.setSubjectDetails(subjectDetails);
            result.setExamType(examType);
            result.setPaperType(paperType);

            // ✅ Step 2: Set totalObtainedMarks from the map
            String key = studentId + "-" + examTypeId + "-" + paperTypeId;
            result.setTotalObtainedMarks(totalMarksMap.get(key));

            // ✅ Step 3: Calculate percentage
            int obtainedMarks = result.getObtainedMarks();
            int totalMarks = subjectDetails.getTotalMarks();
            double percentage = (totalMarks > 0) ? (obtainedMarks * 100.0) / totalMarks : 0.0;
            result.setPercentage(percentage);

            // ✅ Step 4: Determine pass/fail status
            int passingMarks = subjectDetails.getPassingMarks();
            result.setStatus(obtainedMarks >= passingMarks ? "Pass" : "Fail");

            savedResults.add(result);
        }

        return repository.saveAll(savedResults);
    }


    @Override
//    @Cacheable(value = "allStudentSubjectResults", key = "#branchCode")
    public List<StudentSubjectResult> getAll(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view");
        }
        return repository.findAllByBranchCode(branchCode);
    }

    @Override
//    @Cacheable(value = "studentSubjectResultById", key = "#id + '-' + #role + '-' + #email")
    public StudentSubjectResult getById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view");
        }
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("StudentSubjectResult not found"));
    }

    @Override
//    @Cacheable(value = "studentSubjectResultsByStudentId", key = "#studentId + '-' + #role + '-' + #email")
    public List<StudentSubjectResult> getByStudentId(Long studentId, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view");
        }

        return repository.findAllByStudentId(studentId);
    }


    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "studentSubjectResultById", key = "#id + '-' + #role + '-' + #email"),
//            @CacheEvict(value = "allStudentSubjectResults", allEntries = true),
//            @CacheEvict(value = "studentSubjectResultsByStudentId", allEntries = true),
//            @CacheEvict(value = "teacherStudentResults", allEntries = true),
//            @CacheEvict(value = "passFailCountsCache", allEntries = true)
//    })
    public StudentSubjectResult update(Long id, StudentSubjectResult result, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update");
        }

        StudentSubjectResult existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("StudentSubjectResult not found"));

        // Set role and email
        existing.setRole(role);
        existing.setCreatedByEmail(email);

        // Set branch code
        String branchCode;
        if ("teacher".equalsIgnoreCase(role)) {
            branchCode = admissionTeacherRepository.findByEmail(email)
                    .map(AdmissionTeacher::getBranchCode)
                    .orElseThrow(() -> new RuntimeException("Branch code not found for teacher"));
        } else {
            branchCode = fetchBranchCodeByRole(role, email);
        }
        existing.setBranchCode(branchCode);

        // Update obtained marks
        int obtainedMarks = result.getObtainedMarks();
        existing.setObtainedMarks(obtainedMarks);

        // Fetch and update associated entities
        Long studentId = result.getStudent() != null ? result.getStudent().getId() : null;
        Long subjectDetailsId = result.getSubjectDetails() != null ? result.getSubjectDetails().getId() : null;
        Long examTypeId = result.getExamType() != null ? result.getExamType().getId() : null;
        Long paperTypeId = result.getPaperType() != null ? result.getPaperType().getId() : null;

        if (studentId != null) {
            AdmissionForm student = admissionFormRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            existing.setStudent(student);
        }

        if (subjectDetailsId != null) {
            ClassRoomSubjectDetails subjectDetails = subjectDetailsRepository.findById(subjectDetailsId)
                    .orElseThrow(() -> new RuntimeException("SubjectDetails not found"));
            existing.setSubjectDetails(subjectDetails);
        }

        if (examTypeId != null) {
            AdmissionExamType examType = examTypeRepository.findById(examTypeId)
                    .orElseThrow(() -> new RuntimeException("ExamType not found"));
            existing.setExamType(examType);
        }

        if (paperTypeId != null) {
            AdmissionPaperType paperType = paperTypeRepository.findById(paperTypeId)
                    .orElseThrow(() -> new RuntimeException("PaperType not found"));
            existing.setPaperType(paperType);
        }

        // Validate associations
        if (existing.getSubjectDetails() != null && existing.getExamType() != null && existing.getPaperType() != null) {
            ClassRoomSubjectDetails subjectDetails = existing.getSubjectDetails();
            AdmissionClassRoom classroom = subjectDetails.getClassroom();
            AdmissionSubject subject = subjectDetails.getSubject();
            AdmissionExamType examType = existing.getExamType();
            AdmissionPaperType paperType = existing.getPaperType();

            List<ClassRoomSubjectDetails> validCombos =
                    subjectDetailsRepository.findByClassroomAndSubjectAndExamTypeAndPaperType(
                            classroom, subject, examType, paperType);

            if (validCombos.isEmpty()) {
                throw new RuntimeException("Invalid ExamType or PaperType for the given Classroom and Subject");
            }

            // Recalculate percentage and pass/fail
            int totalMarks = subjectDetails.getTotalMarks();
            int passingMarks = subjectDetails.getPassingMarks();
            double percentage = (totalMarks > 0) ? (obtainedMarks * 100.0) / totalMarks : 0.0;

            existing.setPercentage(percentage);
            existing.setStatus(obtainedMarks >= passingMarks ? "Pass" : "Fail");
        }

        // Set totalObtainedMarks for same student-exam-paper
        if (existing.getStudent() != null && existing.getExamType() != null && existing.getPaperType() != null) {
            Long sId = existing.getStudent().getId();
            Long eId = existing.getExamType().getId();
            Long pId = existing.getPaperType().getId();

            List<StudentSubjectResult> allResultsForSameExam = repository
                    .findByStudentIdAndExamTypeIdAndPaperTypeId(sId, eId, pId);

            int total = allResultsForSameExam.stream()
                    .mapToInt(StudentSubjectResult::getObtainedMarks)
                    .sum();

            existing.setTotalObtainedMarks(total);
        }

        return repository.save(existing);
    }


    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "studentSubjectResultById", key = "#id + '-' + #role + '-' + #email"),
//            @CacheEvict(value = "allStudentSubjectResults", allEntries = true),
//            @CacheEvict(value = "studentSubjectResultsByStudentId", allEntries = true),
//            @CacheEvict(value = "teacherStudentResults", allEntries = true),
//            @CacheEvict(value = "passFailCountsCache", allEntries = true)
//    })
    public void delete(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("StudentSubjectResult not found"));
        repository.deleteById(id);
    }

    @Override
    public StudentResultResponse getStudentResultsByStudentId(Long studentId, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view student result");
        }

        // Step 1: Fetch all results for the student
        List<StudentSubjectResult> results = repository.findAllByStudentId(studentId);

        if (results.isEmpty()) {
            throw new EntityNotFoundException("No results found for student ID: " + studentId);
        }

        // Step 2: Get the student object from the first result (they are all same)
        AdmissionForm student = results.get(0).getStudent();

        // Step 3: Get classroom from first subject details
        ClassRoomSubjectDetails firstSubjectDetails = results.get(0).getSubjectDetails();
        AdmissionClassRoom classroom = firstSubjectDetails != null ? firstSubjectDetails.getClassroom() : null;

        // Step 4: Build SubjectResultDto list
        List<SubjectResultDto> subjectResults = results.stream()
                .map(r -> SubjectResultDto.builder()
                        .id(r.getId())
                        .subjectName(r.getSubjectDetails().getSubject().getSubjectName())
                        .examType(r.getExamType().getExamType())
                        .paperType(r.getPaperType().getPaperType())
                        .obtainedMarks(r.getObtainedMarks())
                        .totalMarks(r.getSubjectDetails().getTotalMarks())
                        .passingMarks(r.getSubjectDetails().getPassingMarks())
                        .topicName(r.getSubjectDetails().getTopicName())
                        .build())
                .collect(Collectors.toList());

        // Step 5: Calculate total obtained marks (max of totalObtainedMarks across all entries)
        int totalObtained = results.stream()
                .mapToInt(r -> r.getObtainedMarks())
                .sum();


        // Step 6: Calculate total subject marks (sum of all subjects)
        int totalSubjectMarks = results.stream()
                .mapToInt(r -> r.getSubjectDetails() != null ? r.getSubjectDetails().getTotalMarks() : 0)
                .sum();

        // Step 7: Build final response
        return StudentResultResponse.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .email(student.getEmail())
                .academicYear(classroom != null ? classroom.getAcademicYear() : null)
                .mediumName(student.getMediumName())
                .coursename(student.getCoursename())
                .rollno(student.getRollNo())
                .batchName(classroom != null ? classroom.getBatchName() : null)
                .totalObtainedMarks(totalObtained)
                .totalSubjectMarks(totalSubjectMarks)
                .percentage((totalSubjectMarks > 0) ? (totalObtained * 100.0) / totalSubjectMarks : 0.0)
                .status(subjectResults.stream()
                        .anyMatch(r -> r.getObtainedMarks() < r.getPassingMarks()) ? "Fail" : "Pass")
                .subjectResults(subjectResults)
                .resultDate(results.get(0).getResultDate()) // Assuming such a method exists
                .build();
    }


    @Override
//    @Cacheable(value = "teacherStudentResults", key = "#email")
    public List<StudentResultResponse> getAllStudentResults(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view student results");
        }

        // Step 1: Fetch all results by branch
        List<StudentSubjectResult> allResults = repository.findAllByBranchCode(branchCode)
                .stream()
                .sorted(Comparator.comparing(r -> r.getStudent().getId()))
                .collect(Collectors.toList());

        // ✅ Step 2: Filter results - keep only those classrooms where this teacher is assigned
        List<StudentSubjectResult> filteredResults = allResults.stream()
                .filter(result -> {
                    ClassRoomSubjectDetails details = result.getSubjectDetails();
                    if (details == null || details.getClassroom() == null) return false;

                    AdmissionClassRoom classroom = details.getClassroom();
                    // Check if teacher is assigned to this classroom
                    return classroom.getTeachers().stream()
                            .anyMatch(teacher -> teacher.getEmail().equalsIgnoreCase(email));
                })
                .collect(Collectors.toList());

        // Step 3: Group filtered results by student
        Map<AdmissionForm, List<StudentSubjectResult>> resultsByStudent = filteredResults.stream()
                .collect(Collectors.groupingBy(
                        StudentSubjectResult::getStudent,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<StudentResultResponse> responseList = new ArrayList<>();

        for (Map.Entry<AdmissionForm, List<StudentSubjectResult>> entry : resultsByStudent.entrySet()) {
            AdmissionForm student = entry.getKey();
            List<StudentSubjectResult> results = entry.getValue();

            ClassRoomSubjectDetails firstSubjectDetails = results.isEmpty() ? null : results.get(0).getSubjectDetails();
            AdmissionClassRoom classroom = firstSubjectDetails != null ? firstSubjectDetails.getClassroom() : null;

            List<SubjectResultDto> subjectResults = results.stream()
                    .map(r -> SubjectResultDto.builder()
                            .id(r.getId())
                            .subjectName(r.getSubjectDetails().getSubject().getSubjectName())
                            .examType(r.getExamType().getExamType())
                            .paperType(r.getPaperType().getPaperType())
                            .obtainedMarks(r.getObtainedMarks())
                            .totalMarks(r.getSubjectDetails().getTotalMarks())
                            .passingMarks(r.getSubjectDetails().getPassingMarks())
                            .topicName(r.getSubjectDetails().getTopicName())
                            .build())
                    .collect(Collectors.toList());

            int totalObtained = results.stream()
                    .mapToInt(r -> r.getObtainedMarks() != 0 ? r.getObtainedMarks() : 0)
                    .sum();

            int totalSubjectMarks = results.stream()
                    .mapToInt(r -> r.getSubjectDetails() != null ? r.getSubjectDetails().getTotalMarks() : 0)
                    .sum();

            StudentResultResponse studentResult = StudentResultResponse.builder()
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .email(student.getEmail())
                    .academicYear(classroom != null ? classroom.getAcademicYear() : null)
                    .mediumName(student.getMediumName())
                    .coursename(student.getCoursename())
                    .rollno(student.getRollNo())
                    .batchName(classroom != null ? classroom.getBatchName() : null)
                    .totalObtainedMarks(totalObtained)
                    .totalSubjectMarks(totalSubjectMarks)
                    .percentage((totalSubjectMarks > 0) ? (totalObtained * 100.0) / totalSubjectMarks : 0.0)
                    .status(subjectResults.stream()
                            .anyMatch(r -> r.getObtainedMarks() < r.getPassingMarks()) ? "Fail" : "Pass")
                    .subjectResults(subjectResults)
                    .resultDate(results.get(0).getResultDate())
                    .build();

            responseList.add(studentResult);
        }

        return responseList;
    }

    @Override
//    @Cacheable(value = "passFailCountsCache", key = "#role + '-' + #email + '-' + (#examTypeParam!=null?#examTypeParam:'') + '-' + (#paperTypeParam!=null?#paperTypeParam:'')")
    public List<Map<String, Object>> getPassFailCounts(String role, String email, String examTypeParam, String paperTypeParam) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view result stats.");
        }

        String branchCode = !"TEACHER".equalsIgnoreCase(role)
                ? fetchBranchCodeByRole(role, email)
                : null;

        List<StudentSubjectResult> results = repository.findAll();

        // Filter by role
        if (!"TEACHER".equalsIgnoreCase(role)) {
            results = results.stream()
                    .filter(r -> branchCode.equals(r.getBranchCode()))
                    .toList();
        } else {
            results = results.stream()
                    .filter(r -> email.equalsIgnoreCase(r.getCreatedByEmail()))
                    .toList();
        }

        // Optional filters
        if (examTypeParam != null && !examTypeParam.isBlank()) {
            results = results.stream()
                    .filter(r -> r.getExamType().getExamType().equalsIgnoreCase(examTypeParam))
                    .toList();
        }

        if (paperTypeParam != null && !paperTypeParam.isBlank()) {
            results = results.stream()
                    .filter(r -> r.getPaperType().getPaperType().equalsIgnoreCase(paperTypeParam))
                    .toList();
        }

        // Group by student
        Map<AdmissionForm, List<StudentSubjectResult>> groupedByStudent = results.stream()
                .collect(Collectors.groupingBy(StudentSubjectResult::getStudent));

        long passCount = 0;
        long failCount = 0;

        for (List<StudentSubjectResult> studentResults : groupedByStudent.values()) {
            boolean hasFailed = studentResults.stream()
                    .anyMatch(r -> r.getObtainedMarks() < r.getSubjectDetails().getPassingMarks());

            if (hasFailed) {
                failCount++;
            } else {
                passCount++;
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("examType", examTypeParam != null ? examTypeParam : "ALL");
        map.put("paperType", paperTypeParam != null ? paperTypeParam : "ALL");
        map.put("passCount", passCount);
        map.put("failCount", failCount);

        return List.of(map);
    }

    @Override
    public StudentResultResponse getStudentResultsByStudentId(Long studentId) {
        // Step 1: Fetch all results for the student
        List<StudentSubjectResult> results = repository.findAllByStudentId(studentId);

        if (results.isEmpty()) {
            throw new EntityNotFoundException("No results found for student ID: " + studentId);
        }

        // Step 2: Get the student object from the first result (they are all same)
        AdmissionForm student = results.get(0).getStudent();

        // Step 3: Get classroom from first subject details
        ClassRoomSubjectDetails firstSubjectDetails = results.get(0).getSubjectDetails();
        AdmissionClassRoom classroom = firstSubjectDetails != null ? firstSubjectDetails.getClassroom() : null;

        // Step 4: Build SubjectResultDto list
        List<SubjectResultDto> subjectResults = results.stream()
                .map(r -> SubjectResultDto.builder()
                        .subjectName(r.getSubjectDetails().getSubject().getSubjectName())
                        .examType(r.getExamType().getExamType())
                        .paperType(r.getPaperType().getPaperType())
                        .obtainedMarks(r.getObtainedMarks())
                        .totalMarks(r.getSubjectDetails().getTotalMarks())
                        .passingMarks(r.getSubjectDetails().getPassingMarks())
                        .build())
                .collect(Collectors.toList());

        // Step 5: Calculate total obtained marks
        int totalObtained = results.stream()
                .mapToInt(r -> r.getObtainedMarks())
                .sum();


        // Step 6: Calculate total subject marks
        int totalSubjectMarks = results.stream()
                .mapToInt(r -> r.getSubjectDetails() != null ? r.getSubjectDetails().getTotalMarks() : 0)
                .sum();

        // Step 7: Build final response
        return StudentResultResponse.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .email(student.getEmail())
                .academicYear(classroom != null ? classroom.getAcademicYear() : null)
                .mediumName(student.getMediumName())
                .coursename(student.getCoursename())
                .rollno(student.getRollNo())
                .batchName(classroom != null ? classroom.getBatchName() : null)
                .totalObtainedMarks(totalObtained)
                .totalSubjectMarks(totalSubjectMarks)
                .percentage((totalSubjectMarks > 0) ? (totalObtained * 100.0) / totalSubjectMarks : 0.0)
                .status(subjectResults.stream()
                        .anyMatch(r -> r.getObtainedMarks() < r.getPassingMarks()) ? "Fail" : "Pass")
                .subjectResults(subjectResults)
                .resultDate(results.get(0).getResultDate())
                .build();
    }

}
