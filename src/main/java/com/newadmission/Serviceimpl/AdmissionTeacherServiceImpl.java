package com.newadmission.Serviceimpl;

//import com.newadmission.DTO.TeacherAttendanceSummaryDTO;
import com.newadmission.Entity.AdmissionAttendance;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.JWT.LoginRequest;
import com.newadmission.JWT.LoginResponse;
import com.newadmission.Repository.AdmissionAttendanceRepository;
import com.newadmission.Repository.AdmissionClassRoomRepository;
import com.newadmission.Repository.AdmissionRepository;
import com.newadmission.Repository.AdmissionTeacherRepository;
import com.newadmission.Service.AdmissionTeacherService;
import com.newadmission.Service.EmailService;
import com.newadmission.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdmissionTeacherServiceImpl implements AdmissionTeacherService {

    private final AdmissionTeacherRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private final AdmissionClassRoomRepository classRoomRepository;
    @Autowired
    private OtpService otpService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private AdmissionRepository admissionRepository;

    @Autowired
    private AdmissionAttendanceRepository attendanceRepository;


    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionTeacherServiceImpl(AdmissionTeacherRepository repository,
                                       WebClient webClient,
                                       StaffService staffService,JwtUtil jwtUtil,PasswordEncoder passwordEncoder,AdmissionClassRoomRepository classRoomRepository) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.classRoomRepository= classRoomRepository;
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
            case "TEACHER" -> {
                boolean exists = repository.existsByEmail(email);
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
//    @CacheEvict(value = {"allTeachers", "teacherById"}, allEntries = true)
    public AdmissionTeacher createTeacher(AdmissionTeacher teacher, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create teacher");
        }

        String branchCode = fetchBranchCodeByRole(role, email);
        teacher.setRole(role);
        teacher.setCreatedByEmail(email);
        teacher.setBranchCode(branchCode);

        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));

        return repository.save(teacher);
    }

    @Override
//    @Cacheable(value = "allTeachers", key = "#branchCode + '-' + #role + '-' + #email", unless = "#result == null || #result.isEmpty()")
    public List<AdmissionTeacher> getAllTeachers(String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view teachers");
        }

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
//    @Cacheable(value = "teacherById", key = "#id + '-' + #role + '-' + #email", unless = "#result == null")
    public AdmissionTeacher getTeacherById(int id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view teacher");
        }

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
    }

    @Override
//    @CacheEvict(value = {"allTeachers", "teacherById"}, allEntries = true)
    public AdmissionTeacher updateTeacher(int id, AdmissionTeacher teacher, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update teacher");
        }

        AdmissionTeacher existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (teacher.getTeacherName() != null) {
            existing.setTeacherName(teacher.getTeacherName());
        }

        if (teacher.getEmail() != null) {
            existing.setEmail(teacher.getEmail());
        }

        // Do not update password under any condition

        return repository.save(existing);
    }

    @Override
//    @CacheEvict(value = {"allTeachers", "teacherById"}, allEntries = true)
    public void deleteTeacher(int id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete teacher");
        }

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        repository.deleteById(id);
    }


    @Override
    public LoginResponse login(LoginRequest request) {
        AdmissionTeacher teacher = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), teacher.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(teacher.getEmail());

        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("id", teacher.getId());
        teacherData.put("name", teacher.getTeacherName());
        teacherData.put("email", teacher.getEmail());
        teacherData.put("role", teacher.getRole());
        teacherData.put("branchCode", teacher.getBranchCode());

        return new LoginResponse(token, teacherData);
    }


    @Override
    public List<AdmissionClassRoom> getClassRoomsByTeacherEmail(String teacherEmail, String role, String email) {
        if (!repository.existsByEmail(email)) {
            throw new AccessDeniedException("Teacher not authorized");
        }

        return classRoomRepository.findByTeacherEmail(teacherEmail);
    }

    @Override
    public String sendOtpToEmail(String email) {
        AdmissionTeacher teacher = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String otp = String.valueOf((int)(Math.random() * 900000 + 100000));
        otpService.saveOtp(email, otp);
        emailService.sendOtpEmail(email, otp);

        return "OTP sent to your email";
    }

    @Override
    public String resetPassword(String email, String otp, String newPassword) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        AdmissionTeacher teacher = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        teacher.setPassword(passwordEncoder.encode(newPassword));
        repository.save(teacher);

        return "Password reset successfully";
    }

    @Override
    public List<AdmissionForm> getStudentsByTeacherAndClassroom(Long classId, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view students");
        }

        String branchCode;
        if ("teacher".equalsIgnoreCase(role)) {
            branchCode = repository.findByEmail(email)
                    .map(AdmissionTeacher::getBranchCode)
                    .orElseThrow(() -> new RuntimeException("Branch code not found for teacher"));
        } else {
            branchCode = fetchBranchCodeByRole(role, email);
        }

        // ✅ email itself is the teacherEmail
        return admissionRepository.findByTeacherAndClassroom(email, classId, branchCode);
    }


    @Override
    public long getClassroomCountByTeacherEmail(String teacherEmail, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view classroom count");
        }

        // Optional: if teacher is accessing their own count
        if ("TEACHER".equalsIgnoreCase(role) && !teacherEmail.equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Teacher can only access their own classroom count");
        }

        return classRoomRepository.countByTeacherEmail(teacherEmail);
    }

    @Override
    public List<AdmissionAttendance> getAttendanceByAdmissionFormId(Long admissionFormId, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view attendance");
        }

        if (!"TEACHER".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Only teachers can view attendance");
        }

        AdmissionTeacher teacher = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        AdmissionForm student = admissionRepository.findById(admissionFormId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!teacher.getBranchCode().equalsIgnoreCase(student.getBranchCode())) {
            throw new AccessDeniedException("Not authorized to access this student's attendance");
        }

        return attendanceRepository.findAttendance(
                student.getRollNo(),
                student.getAdmissionClassRoom().getId().intValue(),  // ✅ fixed
                student.getBranchCode()
        );

    }

//    @Override
//    public List<TeacherAttendanceSummaryDTO> getAttendanceSummary(String email, String role) {
//
//        // ✅ Permission check
//        if (!hasPermission(role, email, "GET")) {
//            throw new AccessDeniedException("No permission to view attendance");
//        }
//
//        // Teacher can only view their own classes
//        String teacherEmail = email;
//
//        List<AdmissionClassRoom> classrooms = classRoomRepository.findByTeacherEmail(teacherEmail);
//
//        return classrooms.stream().map(c -> {
//            int classroomId = c.getId().intValue(); // Long → int conversion if needed
//
//            long totalStudents = attendanceRepository.findByClassroomId(classroomId).stream()
//                    .map(AdmissionAttendance::getRollno)
//                    .distinct()
//                    .count();
//
//            long presentCount = attendanceRepository.findByClassroomId(classroomId).stream()
//                    .filter(a -> "Present".equalsIgnoreCase(a.getLoginStatus()))
//                    .count();
//
//            long absentCount = totalStudents - presentCount;
//
//            return new TeacherAttendanceSummaryDTO(
//                    c.getAcademicYear(),
//                    c.getBatchName(),
//                    totalStudents,
//                    presentCount,
//                    absentCount
//            );
//        }).collect(Collectors.toList());
//    }
}