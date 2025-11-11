package com.newadmission.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.newadmission.DTO.*;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionController {

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping(value = "/createAdmission", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdmissionForm> createAdmission(
            @RequestPart("admission") String admissionJson,
            @RequestPart(value = "studentImage", required = false) MultipartFile studentImage,
            @RequestPart(value = "paymentImage", required = false) MultipartFile paymentImage, // ✅ NEW
            @RequestParam(required = false) MultipartFile admissionPdf,
            @RequestParam("role") String role,
            @RequestParam(value = "email", required = false) String email,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        AdmissionForm admission = objectMapper.readValue(admissionJson, AdmissionForm.class);

        List<Integer> rulesIdsInt = (List<Integer>) ((Map<String, Object>) objectMapper.readValue(admissionJson, Map.class)).get("rulesIds");
        List<Long> rulesIds = rulesIdsInt != null ? rulesIdsInt.stream().map(Integer::longValue).toList() : null;

        AdmissionForm saved = admissionService.createAdmission(
                admission, role, email, studentImage, paymentImage, admissionPdf, rulesIds, authorizationHeader
        );
        return ResponseEntity.ok(saved);
    }



    @PostMapping("/getAllAdmissions")
    public ResponseEntity<Map<String, Object>> getAllAdmissions(@RequestBody AdmissionFilterRequest request) {
        Map<String, Object> response = admissionService.getAllAdmissions(request);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/getAdmissionById/{id}")
    public ResponseEntity<AdmissionForm> getAdmissionById(@PathVariable Long id,
                                                          @RequestParam String role,
                                                          @RequestParam String email) {
        return ResponseEntity.ok(admissionService.getAdmissionById(id, role, email));
    }

    @PutMapping(value = "/updateAdmission/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdmissionForm> updateAdmission(
            @PathVariable Long id,
            @RequestPart(value = "admission", required = false) String admissionJson,
            @RequestPart(value = "studentImage", required = false) MultipartFile studentImage,
            @RequestPart(value = "paymentImage", required = false) MultipartFile paymentImage,

            @RequestParam("role") String role,
            @RequestParam("email") String email
    ) {
        AdmissionForm updatedAdmissionForm = admissionService.updateAdmission(id, admissionJson, studentImage,paymentImage, role, email);
        return ResponseEntity.ok(updatedAdmissionForm);
    }

    @DeleteMapping("/deleteAdmission/{id}")
    public ResponseEntity<String> deleteAdmission(@PathVariable Long id,
                                                  @RequestParam String role,
                                                  @RequestParam String email) {
        admissionService.deleteAdmission(id, role, email);
        return ResponseEntity.ok("AdmissionForm deleted successfully");
    }

    @GetMapping("/getAdmissionsByDateFilter")
    public ResponseEntity<List<AdmissionForm>> getAdmissionsByDateFilter(
            @RequestParam String filterType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam(required = false) String branchCode
    ) {
        List<AdmissionForm> results = admissionService.getAdmissionsByDateFilter(
                filterType, startDate, endDate, role, email, branchCode
        );
        return ResponseEntity.ok(results);
    }





    @GetMapping("/filterstudents")
    public ResponseEntity<List<AdmissionForm>> getFilteredStudents(
            @RequestParam String academicYear,
            @RequestParam String mediumName,
            @RequestParam String courseName,
            @RequestParam String role,
            @RequestParam String email
    ) {
        List<AdmissionForm> filtered = admissionService.filterStudentsByClassroom(
                academicYear, mediumName, courseName, role, email);
        return ResponseEntity.ok(filtered);
    }
    @GetMapping("/getAdmissionsByClassroomId")
    public ResponseEntity<List<AdmissionForm>> getAdmissionsByClassroomId(
            @RequestParam Long classroomId,
            @RequestParam String role,
            @RequestParam String email) {

        List<AdmissionForm> admissions = admissionService.getAdmissionsByClassroomId(classroomId, role, email);
        return ResponseEntity.ok(admissions);
    }

    @GetMapping("/getAdmissionStats")
    public ResponseEntity<Map<String, Object>> getAdmissionStats(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam(required = false) String branchCode
    ) {
        Map<String, Object> stats = admissionService.getAdmissionStats(role, email, branchCode);
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/getDailyStats")
    public ResponseEntity<?> getDailyStats(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) String branchCode
    ) {
        Map<String, Map<String, Object>> stats = admissionService.getDailyAdmissionStats(role, email, month, year, branchCode);
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/getMonthlyStats")
    public ResponseEntity<?> getMonthlyStats(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam int year,
            @RequestParam(required = false) String branchCode
    ) {
        Map<String, Map<String, Object>> stats = admissionService.getMonthlyAdmissionStats(role, email, year, branchCode);
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/compareTwoYearsStats")
    public ResponseEntity<Map<String, Map<String, Object>>> compareTwoYearsStats(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam int year1,
            @RequestParam int year2,
            @RequestParam(required = false) String branchCode
    ) {
        Map<String, Map<String, Object>> stats = admissionService.getTwoYearComparisonStats(role, email, year1, year2, branchCode);
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/admissionsbycourse")
    public ResponseEntity<Map<String, Map<String, Object>>> getAdmissionCountAndRevenueByCourseName(
            @RequestParam int year,
            @RequestParam String month,
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String branchCode) {

        Map<String, Map<String, Object>> stats = admissionService.getAdmissionCountAndRevenueByCourseName(
                year, month, role, email, startDate, endDate, branchCode);

        return ResponseEntity.ok(stats);
    }


    @GetMapping("/admissionsbysource")
    public ResponseEntity<Map<String, Long>> getAdmissionsCountBySourceBy(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam(required = false) String branchCode,
            @RequestParam String timeFrame, //today,7days,30days
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<String, Long> result = admissionService.getAdmissionsCountBySourceBy(
                role, email, branchCode, timeFrame, startDate, endDate
        );
        return ResponseEntity.ok(result);
    }



    @GetMapping("/getAdmissionsByTeacherEmail")
    public ResponseEntity<List<AdmissionForm>> getAdmissionsByTeacherEmail(
            @RequestParam String email,
            @RequestParam String role,
            @RequestParam String branchCode) {

        List<AdmissionForm> admissions = admissionService.getAdmissionsByTeacherEmail(email, role, branchCode);
        return ResponseEntity.ok(admissions);
    }
    @PostMapping("/userlogin")
    public AdmissionLoginResponse login(@RequestBody AdmissionLoginRequest request) {
        return admissionService.login(request);
    }

    @PostMapping("/sendotp")
    public ResponseEntity<String> sendOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(admissionService.sendOtpToEmail(body.get("email")));
    }

    @PostMapping("/resetuserpassword")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(admissionService.resetPassword(
                body.get("email"),
                body.get("otp"),
                body.get("newPassword")
        ));
    }

    @GetMapping("/usergetById/{id}")
    public AdmissionForm getAdmissionById(@PathVariable Long id) {
        return admissionService.getAdmissionById(id);
    }


    @GetMapping("/getuserinfo")
    public AdmissionForm getAdmissionByParams(
            @RequestParam Long classroomId,
            @RequestParam Integer rollNo,
            @RequestParam String branchCode) {

        return admissionService.getAdmissionByClassroomRollNoAndBranchCode(classroomId, rollNo, branchCode);
    }

    @PostMapping("/uploadcsv")
    public ResponseEntity<?> uploadAdmissionCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("role") String role,
            @RequestParam("email") String email
    ) {
        try {
            List<String> result = admissionService.uploadAdmissionsFromCsv(file, role, email);

            // If errors exist → send 400
            if (result.size() == 1 && result.get(0).startsWith("CSV uploaded successfully")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    List.of("CSV Upload failed: " + e.getMessage())
            );
        }
    }


    @PutMapping("/removestudents")
    public ResponseEntity<String> removeStudentsFromClassroom(
            @RequestParam Long classroomId,
            @RequestParam List<Long> admissionFormIds,
            @RequestParam String email,
            @RequestParam String role) {

        admissionService.removeStudentsFromClassroom(classroomId, admissionFormIds, role, email);
        return ResponseEntity.ok("Students removed from classroom successfully");
    }

    @PostMapping("/parentlogin")
    public ParentLoginResponse login(@RequestBody ParentLoginRequest request) {
        return admissionService.parentLogin(request);
    }

    @PostMapping("/sendotpparent")
    public ResponseEntity<String> sendOtpForParent(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(admissionService.sendOtpToParentEmail(body.get("parentEmail")));
    }

    @PostMapping("/resetpasswordparent")
    public ResponseEntity<String> resetPasswordForParent(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(admissionService.resetParentPassword(
                body.get("parentEmail"),
                body.get("otp"),
                body.get("newPassword")
        ));
    }

    @GetMapping("/countbystaff")
    public ResponseEntity<Map<String, Object>> getAdmissionsCountByStaffInBranch(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        Map<String, Object> result = admissionService.getAdmissionsCountByStaffInBranch(role, email, branchCode,month,year);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/getStaffInfoByBranchCode")
    public List<Map<String, Object>> getStaffBasicInfo(
            @RequestParam(required = false) String branchCode,
            @RequestParam String role,
            @RequestParam String email) {

        return admissionService.getStaffInfo(role, email, branchCode);
    }

}