package com.newadmission.Service;

import com.beust.jcommander.internal.Nullable;
import com.newadmission.DTO.*;
import com.newadmission.Entity.AdmissionAttendance;
import com.newadmission.Entity.AdmissionForm;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdmissionService {
    AdmissionForm createAdmission(AdmissionForm admissionForm, String role, String email, MultipartFile studentImage, MultipartFile paymentImage,MultipartFile admissionPdf, List<Long> rulesIds,String authHeader);
    Map<String, Object> getAllAdmissions(AdmissionFilterRequest request);
    AdmissionForm updateAdmission(Long id, String admissionJson, MultipartFile studentImage, MultipartFile paymentImage,String role, String email);
    void deleteAdmission(Long id, String role, String email);
    AdmissionForm getAdmissionById(Long id, String role, String email);
    List<AdmissionForm> getAdmissionsByDateFilter(
            String filterType,
            LocalDate startDate,
            LocalDate endDate,
            String role,
            String email,
            String branchCode
    );
    List<AdmissionForm> filterStudentsByClassroom(String academicYear, String mediumName, String courseName, String role, String email);
    List<AdmissionForm> getAdmissionsByClassroomId(Long classroomId, String role, String email);
    Map<String, Object> getAdmissionStats(String role, String email, String branchCode);
    Map<String, Map<String, Object>> getDailyAdmissionStats(String role, String email, int month, int year, String branchCode);
    Map<String, Map<String, Object>> getMonthlyAdmissionStats(String role, String email, int year, String branchCode);
    Map<String, Map<String, Object>> getTwoYearComparisonStats(String role, String email, int year1, int year2, String branchCode);
    Map<String, Map<String, Object>> getAdmissionCountAndRevenueByCourseName(
            int year, String month, String role, String email,
            LocalDate startDate, LocalDate endDate,
            String branchCode);

    Map<String, Long> getAdmissionsCountBySourceBy(String role, String email, String branchCode, String timeFrame, LocalDate startDate, LocalDate endDate);

    List<AdmissionForm> getAdmissionsByTeacherEmail(String email, String role, String branchCode);
    AdmissionLoginResponse login(AdmissionLoginRequest request);

    String sendOtpToEmail(String email);
    String resetPassword(String email, String otp, String newPassword);

    AdmissionForm getAdmissionById(Long id);
    AdmissionForm getAdmissionByClassroomRollNoAndBranchCode(Long classroomId, Integer rollNo, String branchCode);

    List<String> uploadAdmissionsFromCsv(MultipartFile file, String role, String email);

    void removeStudentsFromClassroom(Long classroomId, List<Long> admissionFormIds, String role, String email);

    ParentLoginResponse parentLogin(ParentLoginRequest request);
    String sendOtpToParentEmail(String parentEmail);
    String resetParentPassword(String parentEmail, String otp, String newPassword);

    Map<String, Object> getAdmissionsCountByStaffInBranch(String role, String email, String branchCode, Integer month, Integer year, String timeframe,
            LocalDate startDate, LocalDate endDate);
    List<Map<String, Object>> getStaffInfo(String role, String email, String branchCode);
    List<Map<String, Object>> getDepartmentInfo(String role, String email, String branchCode);
}