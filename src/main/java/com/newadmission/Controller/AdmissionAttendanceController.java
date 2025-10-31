package com.newadmission.Controller;

import com.newadmission.DTO.AttendanceSummary;
import com.newadmission.Entity.AdmissionAttendance;
import com.newadmission.Service.AdmissionAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionAttendanceController {

    @Autowired
    private AdmissionAttendanceService attendanceService;

    @PostMapping("/markAttendance")
    public ResponseEntity<String> markLoginAttendance(
            @RequestParam("image") MultipartFile image,
            @RequestParam("branch_code") String branchCode
    ) {
        return attendanceService.markLoginAttendance(image, branchCode);
    }

    @PostMapping("/logoutAttendance")
    public ResponseEntity<String> markLogoutAttendance(
            @RequestParam("image") MultipartFile image,
            @RequestParam("branch_code") String branchCode
    ) {
        return attendanceService.markLogoutAttendance(image, branchCode);
    }

    @GetMapping("/byClassroomAndBranch")
    public List<AttendanceSummary> getAllSummary(
            @RequestParam int classroomId,
            @RequestParam String branchCode
    ) {
        return attendanceService.getAllSummaryByClassroomAndBranch(classroomId, branchCode);
    }
    @GetMapping("/getAttendanceByAdmissionId")
    public ResponseEntity<List<AttendanceSummary>> getSummaryByAdmissionId(
            @RequestParam Long admissionId,
            @RequestParam String branchCode
    ) {
        return ResponseEntity.ok(attendanceService.getSummaryByAdmissionIdAndBranchCode(admissionId, branchCode));
    }

    @GetMapping("/attendancefilter")
    public ResponseEntity<Map<String, Object>> getAttendanceSummaryByClassroom(
            @RequestParam int classroomId,
            @RequestParam String filter,
            @RequestParam String branchCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Map<String, Object> summary = attendanceService.getAttendanceSummary(
                classroomId, filter, branchCode, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
    @GetMapping("/byadmissionid/{formId}")
    public List<AdmissionAttendance> getAttendanceByAdmissionFormId(
            @PathVariable Long formId,
            @RequestParam(defaultValue = "total") String filter) {
        return attendanceService.getAttendanceByAdmissionFormId(formId, filter);
    }


    @PostMapping("/manualMarkAttendance")
    public ResponseEntity<String> manualMarkAttendance(
            @RequestParam("classroomId") int classroomId,
            @RequestParam("branch_code") String branch_code,
            @RequestBody List<Integer> rollnos) {

        return attendanceService.manualMarkAttendance(classroomId, branch_code, rollnos);
    }

    @GetMapping("/getAttendanceCountByFormId/{formId}")
    public ResponseEntity<Map<String, Object>> getStudentAttendanceSummary(
            @RequestParam Long admissionId,
            @RequestParam String filter,
            @RequestParam String branchCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Map<String, Object> summary = attendanceService.getStudentAttendanceSummary(
                admissionId, filter, branchCode, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/manuallogout")
    public ResponseEntity<String> manualLogout(
            @RequestParam int classroomId,
            @RequestParam String branch_code,
            @RequestBody List<Integer> rollnos
    ) {
        return attendanceService.manualMarkLogout(classroomId, branch_code, rollnos);
    }

}

