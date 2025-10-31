package com.newadmission.Serviceimpl;

import com.newadmission.DTO.PeriodAssignmentRequest;
import com.newadmission.Entity.*;
import com.newadmission.Repository.*;
import com.newadmission.Service.AdmissionTimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdmissionTimetableServiceImpl implements AdmissionTimetableService {

    @Autowired private AdmissionTimetableRepository repository;
    @Autowired private StaffService staffService;
    @Autowired
    private WebClient webClient;



    @Autowired
    private AdmissionSubjectRepository subjectRepository;
    @Autowired
    private AdmissionPeriodRepository periodRepository;
    @Autowired
    private AdmissionTeacherRepository teacherRepository;

    @Autowired
    private AdmissionClassRoomRepository admissionClassRoomRepository;

    @Autowired
    private AdmissionTimetableRepository timetableRepository;

    @Autowired
    private PeriodAssignmentRepository periodAssignmentRepository;

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
    public AdmissionTimetable createTimetableWithAssignments(String weekday,
                                                             Long classRoomId,
                                                             List<PeriodAssignmentRequest> assignments,
                                                             String role,
                                                             String email) {

        // Permission check
        if(!hasPermission(role,email,"POST"))
            throw new AccessDeniedException("No permission");

        String branchCode = fetchBranchCodeByRole(role,email);

        AdmissionClassRoom classRoom = admissionClassRoomRepository.findById(classRoomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        // Optional: Duplicate weekday check for same classroom
        boolean exists = repository.existsByClassRoomIdAndWeekday(classRoomId, weekday);
        if(exists) {
            throw new RuntimeException("Timetable for this classroom and weekday already exists");
        }

        // Create timetable
        AdmissionTimetable timetable = new AdmissionTimetable();
        timetable.setWeekday(weekday);
        timetable.setBranchCode(branchCode);
        timetable.setCreatedByEmail(email);
        timetable.setRole(role);
        timetable.setClassRoom(classRoom);

        // Add assignments
        for(PeriodAssignmentRequest req : assignments){
            AdmissionPeriod period = periodRepository.findById(req.getPeriodId())
                    .orElseThrow(() -> new RuntimeException("Period not found"));

            AdmissionTeacher teacher = teacherRepository.findById(req.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            AdmissionSubject subject = subjectRepository.findById(req.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("Subject not found"));

            PeriodAssignment pa = new PeriodAssignment();
            pa.setPeriod(period);
            pa.setTeacher(teacher);
            pa.setSubject(subject);
            pa.setTimetable(timetable);

            timetable.getAssignments().add(pa);
        }

        // Save and return
        return repository.save(timetable);
    }

    @Override
    public List<AdmissionTimetable> getAllTimetables(String role, String email, String branchCode) {
        if(!hasPermission(role,email,"GET"))
            throw new AccessDeniedException("No permission");

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionTimetable getTimetableById(int id, String role, String email) {
        if(!hasPermission(role,email,"GET"))
            throw new AccessDeniedException("No permission");

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timetable not found"));
    }

    @Override
    public AdmissionTimetable updateTimetableAssignments(int timetableId,
                                                         List<PeriodAssignmentRequest> assignments,
                                                         String role,
                                                         String email) {

        // Permission check
        if(!hasPermission(role, email, "PUT"))
            throw new AccessDeniedException("No permission");

        AdmissionTimetable timetable = repository.findById(timetableId)
                .orElseThrow(() -> new RuntimeException("Timetable not found"));

        // Optional: Check branch access if needed
        String branchCode = fetchBranchCodeByRole(role,email);
        if(!timetable.getBranchCode().equals(branchCode)) {
            throw new AccessDeniedException("You cannot update timetable of another branch");
        }

        // Clear existing assignments
        timetable.getAssignments().clear();

        // Add new assignments
        for(PeriodAssignmentRequest req : assignments){
            AdmissionPeriod period = periodRepository.findById(req.getPeriodId())
                    .orElseThrow(() -> new RuntimeException("Period not found"));

            AdmissionTeacher teacher = teacherRepository.findById(req.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            AdmissionSubject subject = subjectRepository.findById(req.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("Subject not found"));

            PeriodAssignment pa = new PeriodAssignment();
            pa.setPeriod(period);
            pa.setTeacher(teacher);
            pa.setSubject(subject);
            pa.setTimetable(timetable);

            timetable.getAssignments().add(pa);
        }

        // Save updated timetable
        return repository.save(timetable);
    }

    @Override
    public void deleteTimetable(int id, String role, String email){
        if(!hasPermission(role,email,"DELETE"))
            throw new AccessDeniedException("No permission");

        repository.findById(id).orElseThrow(() -> new RuntimeException("Timetable not found"));
        repository.deleteById(id);
    }

    @Override
    public List<AdmissionTimetable> getTimetablesByClassroom(Integer classRoomId, String role, String email) {
        LocalDate today = LocalDate.now();

        // 1️⃣ Fetch all timetables for the classroom
        List<AdmissionTimetable> timetables = repository.findByClassRoomId(Long.valueOf(classRoomId));

        // 2️⃣ Replace assignments with today's OFF if exists
        for (AdmissionTimetable timetable : timetables) {
            Set<PeriodAssignment> updatedAssignments = new HashSet<>();

            for (PeriodAssignment pa : timetable.getAssignments()) {
                var todayOverride = periodAssignmentRepository
                        .findByTimetableIdAndPeriodIdAndDate(timetable.getId(), pa.getPeriod().getId(), today);

                if (todayOverride.isPresent()) {
                    updatedAssignments.add(todayOverride.get());
                } else {
                    PeriodAssignment paCopy = new PeriodAssignment();
                    paCopy.setId(pa.getId());
                    paCopy.setPeriod(pa.getPeriod());
                    paCopy.setTeacher(pa.getTeacher());
                    paCopy.setSubject(pa.getSubject());
                    paCopy.setStatus("ON");
                    paCopy.setDate(null);
                    updatedAssignments.add(paCopy);
                }
            }

            timetable.setAssignments(updatedAssignments);
        }

        return timetables;
    }


    @Override
    public String markPeriodOff(String role, String email,
                                Integer timetableId,
                                Integer periodId) {

        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to modify timetable");
        }

        LocalDate today = LocalDate.now();

        // 1️⃣ Check if a date-specific OFF assignment already exists
        var existing = periodAssignmentRepository
                .findByTimetableIdAndPeriodIdAndDate(timetableId, periodId, today);

        if (existing.isPresent()) {
            PeriodAssignment period = existing.get();
            if ("OFF".equalsIgnoreCase(period.getStatus())) {
                return "Period already marked as OFF for today";
            } else {
                period.setStatus("OFF");
                periodAssignmentRepository.save(period);
                return "Period updated as OFF for today";
            }
        }

        // 2️⃣ Fetch timetable with assignments
        AdmissionTimetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new RuntimeException("Timetable not found"));

        // 3️⃣ Find base assignment for this period
        PeriodAssignment baseAssignment = timetable.getAssignments().stream()
                .filter(pa -> pa.getPeriod().getId().equals(periodId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Base period assignment not found"));

        // 4️⃣ Create new date-specific OFF assignment
        PeriodAssignment offPeriod = new PeriodAssignment();
        offPeriod.setTimetable(timetable);
        offPeriod.setPeriod(baseAssignment.getPeriod());
        offPeriod.setTeacher(baseAssignment.getTeacher());
        offPeriod.setSubject(baseAssignment.getSubject());
        offPeriod.setDate(today);
        offPeriod.setStatus("OFF");

        periodAssignmentRepository.save(offPeriod);

        return "New period marked as OFF for today";
    }






}
