package com.newadmission.Serviceimpl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newadmission.DTO.*;
import com.newadmission.Entity.AdmissionAttendance;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Repository.AdmissionAttendanceRepository;
import com.newadmission.Repository.AdmissionClassRoomRepository;
import com.newadmission.Repository.AdmissionRepository;
import com.newadmission.Service.AdmissionAttendanceService;
import com.newadmission.Service.GupshupService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdmissionAttendanceServiceImpl implements AdmissionAttendanceService {

    @Value("${admission.attendance.python-api-login-url}")
    private String pythonLoginApiUrl;

    @Value("${admission.attendance.python-api-logout-url}")
    private String pythonLogoutApiUrl;


    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private AdmissionRepository admissionRepository;

    @Autowired
    private AdmissionAttendanceRepository attendanceRepository;

    @Autowired
    private AdmissionClassRoomRepository admissionClassRoomRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private GupshupService gupshupService;

    @Autowired
    private StaffService staffService;




    @Override
    public ResponseEntity<String> markLoginAttendance(MultipartFile image, String branch_code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
            body.add("branch_code", branch_code);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(pythonLoginApiUrl, requestEntity, String.class);

            // Check for API failure
            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(response.getStatusCode())
                        .body("Python API error: " + response.getStatusCode());
            }

            if (response.getBody() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: Python API response body is null.");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root;
            try {
                root = mapper.readTree(response.getBody());
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid JSON received from Python API");
            }

            // No matches
            if ("no_matches".equalsIgnoreCase(root.get("status").asText())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No match found");
            }

            StringBuilder attendanceMsg = new StringBuilder();

            if ("success".equalsIgnoreCase(root.get("status").asText())) {
                JsonNode matches = root.get("matches");
                if (matches.isArray()) {
                    for (JsonNode match : matches) {
                        int rollno = match.get("rollno").asInt();
                        int classroomId = "unknown".equalsIgnoreCase(match.get("classroomId").asText()) ? 0 : match.get("classroomId").asInt();
                        String branch = match.get("branch").asText();
                        LocalDateTime timestamp = LocalDateTime.parse(match.get("timestamp").asText());
                        LocalDate date = timestamp.toLocalDate();
                        LocalTime loginTime = timestamp.toLocalTime();

                        AdmissionForm form = admissionRepository.findByRollNoAndAdmissionClassRoomId(
                                rollno, (long) classroomId
                        );

                        if (form == null || form.getAdmissionClassRoom() == null) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("Invalid student or classroom information.");
                        }

                        AdmissionClassRoom classRoom = form.getAdmissionClassRoom();
                        LocalTime batchStartTime = classRoom.getBatchStartTime();
                        LocalTime batchEndTime = classRoom.getBatchEndTime();

                        // Reject if outside allowed time
                        if (loginTime.isBefore(batchStartTime) || loginTime.isAfter(batchEndTime)) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                    "Login time must be between " + batchStartTime + " and " + batchEndTime
                            );
                        }

                        Optional<AdmissionAttendance> optional = attendanceRepository
                                .findByRollnoAndDateAndClassroomId(rollno, date, classroomId);

                        if (optional.isPresent() && optional.get().getLoginTime() != null) {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body("Attendance already marked for roll number: " + rollno);
                        }

                        String loginStatus = loginTime.isAfter(batchStartTime) ? "Late" : "On Time";

                        if (optional.isPresent()) {
                            AdmissionAttendance existing = optional.get();
                            existing.setLoginTime(loginTime);
                            existing.setLoginStatus(loginStatus);
                            attendanceRepository.save(existing);
                        } else {
                            AdmissionAttendance attendance = AdmissionAttendance.builder()
                                    .branch_code(branch)
                                    .classroomId(classroomId)
                                    .rollno(rollno)
                                    .studentName(form.getName())
                                    .date(date)
                                    .loginTime(loginTime)
                                    .loginStatus(loginStatus)
                                    .build();

                            attendanceRepository.save(attendance);
                        }

                        // ✅ Send WhatsApp notification like manualMarkAttendance
                        try {
                            BulkWhatsAppRequest waRequest = new BulkWhatsAppRequest();
                            List<WhatsAppRecipientDTO> recipients = new ArrayList<>();
                            WhatsAppRecipientDTO recipient = new WhatsAppRecipientDTO();

                            recipient.setPhone(formatPhoneNumber(form.getMobile1()));
                            recipient.setTemplateId("62496d5e-7af9-411a-8fdd-0be50e01b7c9");
                            recipient.setParameters(List.of(
                                    getInstituteNameFromBranchCode(form.getBranchCode()),             // {{1}} Institute Name
                                    form.getName(),                                                    // {{2}} Student Name
                                    form.getCoursename(),                                              // {{3}} Course
                                    form.getAdmissionClassRoom().getBatchName(),                       // {{4}} Batch
                                    String.valueOf(form.getRollNo()),                                  // {{5}} Roll Number
                                    "Logged IN",                                                       // {{6}} Status
                                    date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),            // {{7}} Date
                                    loginTime.format(DateTimeFormatter.ofPattern("h:mm a"))            // {{8}} Time
                            ));

                            recipients.add(recipient);
                            waRequest.setRecipients(recipients);
                            gupshupService.sendWhatsAppTemplate(waRequest);
                        } catch (Exception ex) {
                            System.err.println("Failed to send WhatsApp message: " + ex.getMessage());
                        }

                        attendanceMsg.append("Attendance marked for Roll No: ")
                                .append(rollno)
                                .append(" (")
                                .append(loginStatus)
                                .append(")\n");

                        // Mark Absent for other students
                        if (classroomId != 0) {
                            List<AdmissionForm> allStudents = admissionRepository.findByAdmissionClassRoomId((long) classroomId);
                            if (allStudents != null) {
                                for (AdmissionForm student : allStudents) {
                                    int otherRollNo = student.getRollNo();
                                    if (otherRollNo != rollno) {
                                        Optional<AdmissionAttendance> otherAttendanceOpt =
                                                attendanceRepository.findByRollnoAndDateAndClassroomId(
                                                        otherRollNo, date, classroomId);

                                        if (otherAttendanceOpt.isEmpty()) {
                                            AdmissionAttendance absent = AdmissionAttendance.builder()
                                                    .branch_code(student.getBranchCode())
                                                    .classroomId(classroomId)
                                                    .rollno(otherRollNo)
                                                    .studentName(student.getName())
                                                    .date(date)
                                                    .loginTime(null)
                                                    .logoutTime(null)
                                                    .loginStatus("Absent")
                                                    .build();

                                            attendanceRepository.save(absent);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return ResponseEntity.ok(attendanceMsg.toString().trim());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during attendance marking: " + e.getMessage());
        }
    }






    @Override
    public ResponseEntity<String> markLogoutAttendance(MultipartFile image, String branch_code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
            body.add("branch_code", branch_code);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(pythonLogoutApiUrl, requestEntity, String.class);

            // ✅ API failure check
            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(response.getStatusCode())
                        .body("Python API error: " + response.getStatusCode());
            }

            if (response.getBody() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: Python API response body is null.");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root;
            try {
                root = mapper.readTree(response.getBody());
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid JSON received from Python API");
            }

            // ✅ No matches
            if ("no_matches".equalsIgnoreCase(root.get("status").asText())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No match found");
            }

            StringBuilder logoutMsg = new StringBuilder();

            if ("success".equalsIgnoreCase(root.get("status").asText())) {
                JsonNode matches = root.get("matches");
                if (matches.isArray()) {
                    for (JsonNode match : matches) {
                        int rollno = match.get("rollno").asInt();
                        int classroomId = "unknown".equalsIgnoreCase(match.get("classroomId").asText()) ? 0 : match.get("classroomId").asInt();
                        LocalDateTime timestamp = LocalDateTime.parse(match.get("timestamp").asText());
                        LocalDate date = timestamp.toLocalDate();
                        LocalTime logoutTime = timestamp.toLocalTime();

                        Optional<AdmissionAttendance> optional = attendanceRepository.findByRollnoAndDateAndClassroomId(
                                rollno, date, classroomId);

                        if (optional.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("Logout not allowed: No login record found for today.");
                        }

                        AdmissionAttendance existing = optional.get();

                        if (existing.getLogoutTime() != null) {
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body("Logout already marked for roll number: " + rollno);
                        }

                        AdmissionForm form = admissionRepository.findByRollNoAndAdmissionClassRoomId(
                                rollno, (long) classroomId
                        );

                        if (form == null || form.getAdmissionClassRoom() == null) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("Invalid student or classroom information.");
                        }

                        AdmissionClassRoom classRoom = form.getAdmissionClassRoom();
                        LocalTime batchStartTime = classRoom.getBatchStartTime();
                        LocalTime batchEndTime = classRoom.getBatchEndTime();

                        if (logoutTime.isBefore(batchStartTime) || logoutTime.isAfter(batchEndTime)) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                    "Logout time must be between " + batchStartTime + " and " + batchEndTime
                            );
                        }

                        // ✅ Save logout time
                        existing.setLogoutTime(logoutTime);
                        attendanceRepository.save(existing);

                        // ✅ Send WhatsApp message
                        try {
                            BulkWhatsAppRequest waRequest = new BulkWhatsAppRequest();
                            List<WhatsAppRecipientDTO> recipients = new ArrayList<>();
                            WhatsAppRecipientDTO recipient = new WhatsAppRecipientDTO();

                            recipient.setPhone(formatPhoneNumber(form.getMobile1()));
                            recipient.setTemplateId("62496d5e-7af9-411a-8fdd-0be50e01b7c9");
                            recipient.setParameters(List.of(
                                    getInstituteNameFromBranchCode(form.getBranchCode()),             // {{1}}
                                    form.getName(),                                                    // {{2}}
                                    form.getCoursename(),                                              // {{3}}
                                    form.getAdmissionClassRoom().getBatchName(),                       // {{4}}
                                    String.valueOf(form.getRollNo()),                                  // {{5}}
                                    "Logged OUT",                                                      // {{6}}
                                    date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),            // {{7}}
                                    logoutTime.format(DateTimeFormatter.ofPattern("h:mm a"))           // {{8}}
                            ));

                            recipients.add(recipient);
                            waRequest.setRecipients(recipients);
                            gupshupService.sendWhatsAppTemplate(waRequest);
                        } catch (Exception ex) {
                            System.err.println("Failed to send WhatsApp message: " + ex.getMessage());
                        }

                        logoutMsg.append("Logout marked for Roll No: ").append(rollno).append("\n");
                    }
                }
            }

            return ResponseEntity.ok(logoutMsg.toString().trim());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during logout marking: " + e.getMessage());
        }
    }


    @Override
    public List<AttendanceSummary> getAllSummaryByClassroomAndBranch(int classroomId, String branchCode) {
        return attendanceRepository.getAllSummaryByClassroomAndBranch(classroomId, branchCode);
    }

    @Override
    public List<AttendanceSummary> getSummaryByAdmissionIdAndBranchCode(Long admissionId, String branchCode) {
        return attendanceRepository.getSummaryByAdmissionIdAndBranchCode(admissionId, branchCode);
    }

    @Override
    public AttendanceSummaryWithCountsDTO getAttendanceSummaryWithCounts(int classroomId) {
        LocalDate today = LocalDate.now();
        LocalDate last7 = today.minusDays(7);
        LocalDate last30 = today.minusDays(30);
        LocalDate last365 = today.minusDays(365);

        long total = attendanceRepository.countByClassroomId(classroomId);
        long todayCount = attendanceRepository.countByClassroomIdAndDate(classroomId, today);
        long last7Days = attendanceRepository.countByClassroomIdAndDateBetween(classroomId, last7, today);
        long last30Days = attendanceRepository.countByClassroomIdAndDateBetween(classroomId, last30, today);
        long last365Days = attendanceRepository.countByClassroomIdAndDateBetween(classroomId, last365, today);

        List<AttendanceSummary> summaryData = attendanceRepository.getSummaryByClassroomId(classroomId);

        return AttendanceSummaryWithCountsDTO.builder()
                .total(total)
                .todayCount(todayCount)
                .last7DaysCount(last7Days)
                .last30DaysCount(last30Days)
                .last365DaysCount(last365Days)
                .data(summaryData)
                .build();
    }


    @Override
    public Map<String, Object> getAttendanceSummary(int classroomId, String filter, String branchCode, String startDateStr, String endDateStr) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        switch (filter.toLowerCase()) {
            case "today" -> {
                startDate = today;
                endDate = today;
            }
            case "7days" -> {
                startDate = today.minusDays(6);
                endDate = today;
            }
            case "30days" -> {
                startDate = today.minusDays(29);
                endDate = today;
            }
            case "365days" -> {
                startDate = today.minusDays(364);
                endDate = today;
            }
            case "custom" -> {
                if (startDateStr == null || endDateStr == null) {
                    throw new IllegalArgumentException("Start date and end date must be provided for custom filter");
                }
                startDate = LocalDate.parse(startDateStr);
                endDate = LocalDate.parse(endDateStr);
            }
            default -> throw new IllegalArgumentException("Invalid filter: " + filter);
        }

        // Fetch classroom to get created date
        AdmissionClassRoom classroom = admissionClassRoomRepository.findById((long) classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        LocalDate createdDate = classroom.getCreatedDate();
        if (startDate.isBefore(createdDate)) {
            startDate = createdDate;
        }

        // Fetch students in classroom
        List<AdmissionForm> studentsInClass = admissionRepository
                .findByAdmissionClassRoomIdAndBranchCode((long) classroomId, branchCode);

        // Fetch attendance in date range
        List<AdmissionAttendance> attendances = attendanceRepository
                .findAttendanceByBranchAndClassroomAndDateRange(branchCode, classroomId, startDate, endDate);

        Set<Integer> presentRollNos = attendances.stream()
                .filter(att -> att.getLoginTime() != null || att.getLogoutTime() != null)
                .map(AdmissionAttendance::getRollno)
                .collect(Collectors.toSet());

        List<Map<String, Object>> presentData = attendances.stream()
                .filter(att -> att.getLoginTime() != null || att.getLogoutTime() != null)
                .map(att -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("rollno", att.getRollno());
                    data.put("studentName", att.getStudentName());
                    data.put("loginTime", att.getLoginTime());
                    data.put("logoutTime", att.getLogoutTime());
                    data.put("loginStatus", att.getLoginStatus());
                    data.put("date", att.getDate());
                    return data;
                }).toList();

        LocalDate finalStartDate = startDate;
        List<Map<String, Object>> absentData = studentsInClass.stream()
                .filter(student -> !presentRollNos.contains(student.getRollNo()))
                .map(student -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("rollno", student.getRollNo());
                    data.put("studentName", student.getName());
                    data.put("loginStatus", "Absent");
                    data.put("loginTime", null);
                    data.put("logoutTime", null);
                    data.put("date", finalStartDate.equals(endDate) ? finalStartDate : null); // Optional
                    return data;
                }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("presentCount", presentRollNos.size());
        response.put("absentCount", absentData.size());
        response.put("totalStudents", studentsInClass.size());
        response.put("presentData", presentData);
        response.put("absentData", absentData);

        return response;
    }


    @Override
    public List<AdmissionAttendance> getAttendanceByAdmissionFormId(Long admissionFormId, String filter) {
        AdmissionForm form = admissionRepository.findById(admissionFormId)
                .orElseThrow(() -> new RuntimeException("AdmissionForm not found with id: " + admissionFormId));

        if (form.getAdmissionClassRoom() == null) {
            throw new RuntimeException("No classroom assigned to this AdmissionForm.");
        }

        int classroomId = Math.toIntExact(form.getAdmissionClassRoom().getId());

        LocalDate today = LocalDate.now();

        return switch (filter.toLowerCase()) {
            case "today" -> attendanceRepository.findByClassroomIdAndDate(classroomId, today);
            case "7day" -> attendanceRepository.findByClassroomIdAndDateBetween(classroomId, today.minusDays(6), today);
            case "30day" -> attendanceRepository.findByClassroomIdAndDateBetween(classroomId, today.minusDays(29), today);
            case "365day" -> attendanceRepository.findByClassroomIdAndDateBetween(classroomId, today.minusDays(364), today);
            case "total" -> attendanceRepository.findByClassroomId(classroomId);
            default -> throw new IllegalArgumentException("Invalid filter: " + filter);
        };
    }

    @Override
    @Transactional
    public ResponseEntity<String> manualMarkAttendance(int classroomId, String branch_code, List<Integer> rollnos) {
        try {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // Fetch all students from classroom
            List<AdmissionForm> allStudents = admissionRepository.findByAdmissionClassRoomId((long) classroomId);

            if (allStudents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No students found for the given classroom.");
            }

            // ✅ Force fresh classroom data
            entityManager.clear();
            AdmissionClassRoom classroom = admissionClassRoomRepository.findById((long) classroomId)
                    .orElse(null);

            if (classroom == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Classroom not found.");
            }

            LocalTime batchStartTime = classroom.getBatchStartTime();
            LocalTime batchEndTime = classroom.getBatchEndTime();

            // ✅ Apply buffer
            LocalTime allowedStartTime = batchStartTime.minusMinutes(30);
            LocalTime allowedEndTime = batchEndTime.plusMinutes(30);

            // ✅ Check if now is within the allowed buffer window
            if (now.isBefore(allowedStartTime)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot mark attendance before allowed buffer time.");
            }

            if (now.isAfter(allowedEndTime)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot mark attendance after allowed buffer time.");
            }

            boolean atLeastOneMarked = false;
            int skippedStudents = 0;
            int totalStudents = rollnos.size();

            for (Integer rollno : rollnos) {
                AdmissionForm student = allStudents.stream()
                        .filter(s -> rollno.equals(s.getRollNo()))
                        .findFirst()
                        .orElse(null);

                if (student == null) {
                    skippedStudents++;
                    continue;
                }

                Optional<AdmissionAttendance> existingOpt = attendanceRepository
                        .findByRollnoAndDateAndClassroomId(rollno, today, classroomId);

                if (existingOpt.isPresent()) {
                    AdmissionAttendance existing = existingOpt.get();

                    if (existing.getLoginTime() != null || "Absent".equalsIgnoreCase(existing.getLoginStatus())) {
                        skippedStudents++;
                        continue;
                    }

                    // ✅ Re-fetch classroom with buffer check before marking
                    entityManager.clear();
                    classroom = admissionClassRoomRepository.findById((long) classroomId).orElse(null);
                    batchStartTime = classroom.getBatchStartTime();
                    batchEndTime = classroom.getBatchEndTime();
                    allowedStartTime = batchStartTime.minusMinutes(30);
                    allowedEndTime = batchEndTime.plusMinutes(30);

                    if (now.isBefore(allowedStartTime) || now.isAfter(allowedEndTime)) {
                        skippedStudents++;
                        continue;
                    }

                    existing.setLoginTime(now);
                    existing.setLoginStatus(now.isAfter(batchStartTime) ? "Late" : "On Time");
                    attendanceRepository.save(existing);
                    atLeastOneMarked = true;
                    continue;
                }

                // ✅ Re-fetch classroom before creating new entry
                entityManager.clear();
                classroom = admissionClassRoomRepository.findById((long) classroomId).orElse(null);
                batchStartTime = classroom.getBatchStartTime();
                batchEndTime = classroom.getBatchEndTime();
                allowedStartTime = batchStartTime.minusMinutes(30);
                allowedEndTime = batchEndTime.plusMinutes(30);

                if (now.isBefore(allowedStartTime) || now.isAfter(allowedEndTime)) {
                    skippedStudents++;
                    continue;
                }

                AdmissionAttendance attendance = AdmissionAttendance.builder()
                        .branch_code(student.getBranchCode())
                        .classroomId(classroomId)
                        .rollno(student.getRollNo())
                        .studentName(student.getName())
                        .date(today)
                        .loginTime(now)
                        .logoutTime(null)
                        .loginStatus(now.isAfter(batchStartTime) ? "Late" : "On Time")
                        .build();

                attendanceRepository.save(attendance);
                // ✅ Send WhatsApp notification
                try {
                    BulkWhatsAppRequest waRequest = new BulkWhatsAppRequest();
                    List<WhatsAppRecipientDTO> recipients = new ArrayList<>();
                    WhatsAppRecipientDTO recipient = new WhatsAppRecipientDTO();

                    // ✅ Ensure +91 formatting
                    recipient.setPhone(formatPhoneNumber(student.getMobile1()));

                    recipient.setTemplateId("62496d5e-7af9-411a-8fdd-0be50e01b7c9");
                    recipient.setParameters(List.of(
                            getInstituteNameFromBranchCode(classroom.getBranchCode()), // {{1}} Institute Name
                            student.getName(),                                         // {{2}}
                            classroom.getCourse().getCoursename(),                     // {{3}} Course Name
                            classroom.getBatchName(),                                  // {{4}}
                            String.valueOf(student.getRollNo()),                       // {{5}}
                            "Logged IN",                                               // {{6}}
                            today.format(DateTimeFormatter.ofPattern("d MMM yyyy")),   // {{7}}
                            now.format(DateTimeFormatter.ofPattern("h:mm a"))          // {{8}}
                    ));

                    recipients.add(recipient);
                    waRequest.setRecipients(recipients);
                    gupshupService.sendWhatsAppTemplate(waRequest);
                } catch (Exception ex) {
                    System.err.println("Failed to send WhatsApp message: " + ex.getMessage());
                }
                atLeastOneMarked = true;
            }

            // ✅ Enhanced response logic
            if (!atLeastOneMarked) {
                if (skippedStudents == totalStudents) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Attendance already marked for all selected students.");
                } else if (now.isBefore(allowedStartTime) || now.isAfter(allowedEndTime)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Attendance could not be marked: Current time is outside the allowed buffer window.");
                } else {
                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .body("Attendance marked for some students. Others were already marked.");
                }
            }

            return ResponseEntity.ok("Manual attendance marked successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during manual attendance marking: " + e.getMessage());
        }
    }

    private String getInstituteNameFromBranchCode(String branchCode) {
        try {
            // Step 1: Get institute email
            String instituteEmail = staffService.getInstituteEmailByBranchCode(branchCode).block();
            if (instituteEmail == null || instituteEmail.isBlank()) {
                return "Unknown Institute";
            }

            // Step 2: Get institute details
            List<InstituteLoginResponse> institutes = staffService.getInstituteDetailsOnly(instituteEmail);
            if (institutes != null && !institutes.isEmpty()) {
                return institutes.get(0).getInstituteName();
            }
        } catch (Exception e) {
            System.err.println("Error fetching institute name: " + e.getMessage());
        }
        return "Unknown Institute";
    }

    // ✅ Helper method to ensure phone number has +91 format
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "";
        phone = phone.trim().replaceAll("[^0-9]", ""); // Remove non-numeric
        if (phone.startsWith("91") && phone.length() == 12) {
            return "+" + phone;
        }
        if (phone.length() == 10) {
            return "+91" + phone;
        }
        return "+" + phone;
    }



    @Override
    public Map<String, Object> getStudentAttendanceSummary(Long admissionId, String filter, String branchCode, String startDateStr, String endDateStr) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        switch (filter.toLowerCase()) {
            case "today" -> {
                startDate = today;
                endDate = today;
            }
            case "7days" -> {
                startDate = today.minusDays(6);
                endDate = today;
            }
            case "30days" -> {
                startDate = today.minusDays(29);
                endDate = today;
            }
            case "365days" -> {
                startDate = today.minusDays(364);
                endDate = today;
            }
            case "custom" -> {
                if (startDateStr == null || endDateStr == null) {
                    throw new IllegalArgumentException("Start date and end date must be provided for custom filter");
                }
                startDate = LocalDate.parse(startDateStr);
                endDate = LocalDate.parse(endDateStr);
            }
            default -> throw new IllegalArgumentException("Invalid filter: " + filter);
        }

        // 1. Fetch the student
        AdmissionForm student = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 2. Get attendance for this student in date range
        List<AdmissionAttendance> attendanceList = attendanceRepository.findByRollnoAndBranchAndDateRange(
                student.getRollNo(), student.getBranchCode(), startDate, endDate
        );

        // 3. Determine present entries (based on login/logout times)
        List<AdmissionAttendance> presentData = attendanceList.stream()
                .filter(att -> att.getLoginTime() != null || att.getLogoutTime() != null)
                .toList();

        int presentCount = presentData.size();
        int totalDays = (int) startDate.datesUntil(endDate.plusDays(1)).count(); // inclusive
        int absentCount = totalDays - presentCount;

        Map<String, Object> response = new HashMap<>();
        response.put("studentId", admissionId);
        response.put("studentName", student.getName());
        response.put("presentCount", presentCount);
        response.put("absentCount", absentCount);
        response.put("totalDays", totalDays);
        response.put("dateRange", Map.of("from", startDate, "to", endDate));
        response.put("presentData", presentData);

        return response;
    }

    @Override
    @Transactional
    public ResponseEntity<String> manualMarkLogout(int classroomId, String branch_code, List<Integer> rollnos) {
        try {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // Fetch all students from classroom
            List<AdmissionForm> allStudents = admissionRepository.findByAdmissionClassRoomId((long) classroomId);

            if (allStudents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No students found for the given classroom.");
            }

            // Force fresh classroom data
            entityManager.clear();
            AdmissionClassRoom classroom = admissionClassRoomRepository.findById((long) classroomId)
                    .orElse(null);

            if (classroom == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Classroom not found.");
            }

            boolean atLeastOneMarked = false;
            StringBuilder logoutMsg = new StringBuilder();

            for (AdmissionForm student : allStudents) {
                boolean isLoggingOut = rollnos.contains(student.getRollNo());
                if (!isLoggingOut) continue;

                Optional<AdmissionAttendance> existingOpt = attendanceRepository
                        .findByRollnoAndDateAndClassroomId(student.getRollNo(), today, classroomId);

                if (existingOpt.isPresent()) {
                    AdmissionAttendance existing = existingOpt.get();

                    if (existing.getLogoutTime() != null) {
                        continue; // Already logged out
                    }

                    if (existing.getLoginTime() != null) {
                        existing.setLogoutTime(now);
                        attendanceRepository.save(existing);
                        atLeastOneMarked = true;

                        // ✅ Send WhatsApp message (Logout) like in markLoginAttendance
                        try {
                            BulkWhatsAppRequest waRequest = new BulkWhatsAppRequest();
                            List<WhatsAppRecipientDTO> recipients = new ArrayList<>();
                            WhatsAppRecipientDTO recipient = new WhatsAppRecipientDTO();

                            recipient.setPhone(formatPhoneNumber(student.getMobile1()));
                            recipient.setTemplateId("62496d5e-7af9-411a-8fdd-0be50e01b7c9"); // Same template ID style
                            recipient.setParameters(List.of(
                                    getInstituteNameFromBranchCode(classroom.getBranchCode()),         // {{1}} Institute Name
                                    student.getName(),                                                  // {{2}} Student Name
                                    classroom.getCourse().getCoursename(),                              // {{3}} Course
                                    classroom.getBatchName(),                                           // {{4}} Batch
                                    String.valueOf(student.getRollNo()),                                // {{5}} Roll Number
                                    "Logged OUT",                                                       // {{6}} Status
                                    today.format(DateTimeFormatter.ofPattern("d MMM yyyy")),            // {{7}} Date
                                    now.format(DateTimeFormatter.ofPattern("h:mm a"))                   // {{8}} Time
                            ));

                            recipients.add(recipient);
                            waRequest.setRecipients(recipients);
                            gupshupService.sendWhatsAppTemplate(waRequest);
                        } catch (Exception ex) {
                            System.err.println("Failed to send WhatsApp message: " + ex.getMessage());
                        }

                        logoutMsg.append("Logout marked for Roll No: ")
                                .append(student.getRollNo())
                                .append("\n");
                    }
                }
            }

            if (atLeastOneMarked) {
                return ResponseEntity.ok(logoutMsg.toString().trim());
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("No matching students found for logout or already logged out.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during manual logout marking: " + e.getMessage());
        }
    }




}