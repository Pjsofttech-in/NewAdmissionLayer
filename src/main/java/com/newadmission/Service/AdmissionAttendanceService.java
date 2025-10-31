package com.newadmission.Service;

import com.newadmission.DTO.AttendanceSummary;
import com.newadmission.DTO.AttendanceSummaryWithCountsDTO;
import com.newadmission.Entity.AdmissionAttendance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdmissionAttendanceService {
    ResponseEntity<String> markLoginAttendance(MultipartFile image, String branch_code);
    ResponseEntity<String> markLogoutAttendance(MultipartFile image, String branch_code);


    List<AttendanceSummary> getAllSummaryByClassroomAndBranch(int classroomId, String branchCode);
    List<AttendanceSummary> getSummaryByAdmissionIdAndBranchCode(Long admissionId, String branchCode);

    AttendanceSummaryWithCountsDTO getAttendanceSummaryWithCounts(int classroomId);

    Map<String, Object> getAttendanceSummary(int classroomId, String filter, String branchCode, String startDate, String endDate);


    List<AdmissionAttendance> getAttendanceByAdmissionFormId(Long admissionFormId, String filter);

    ResponseEntity<String> manualMarkAttendance(int classroomId, String branch_code, List<Integer> rollnos);
    ResponseEntity<String> manualMarkLogout(int classroomId, String branch_code, List<Integer> rollnos);


    Map<String, Object> getStudentAttendanceSummary(
            Long admissionId,
            String filter,
            String branchCode,
            String startDateStr,
            String endDateStr
    );

}