package com.newadmission.Controller;

//import com.newadmission.DTO.TeacherAttendanceSummaryDTO;
import com.newadmission.Entity.AdmissionAttendance;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.JWT.LoginRequest;
import com.newadmission.JWT.LoginResponse;
import com.newadmission.Service.AdmissionTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionTeacherController {

    @Autowired
    private AdmissionTeacherService admissionTeacherService;

    @PostMapping("/createTeacher")
    public ResponseEntity<AdmissionTeacher> createTeacher(@RequestBody AdmissionTeacher teacher,
                                                          @RequestParam String role,
                                                          @RequestParam String email) {
        return ResponseEntity.ok(admissionTeacherService.createTeacher(teacher, role, email));
    }

    @GetMapping("/getAllTeachers")
    public ResponseEntity<List<AdmissionTeacher>> getAllTeachers(@RequestParam String role,
                                                                 @RequestParam String email,
                                                                 @RequestParam String branchCode) {
        return ResponseEntity.ok(admissionTeacherService.getAllTeachers(role, email, branchCode));
    }

    @GetMapping("/getTeacherById/{id}")
    public ResponseEntity<AdmissionTeacher> getTeacherById(@PathVariable int id,
                                                           @RequestParam String role,
                                                           @RequestParam String email) {
        return ResponseEntity.ok(admissionTeacherService.getTeacherById(id, role, email));
    }

    @PutMapping("/updateTeacher/{id}")
    public ResponseEntity<AdmissionTeacher> updateTeacher(@PathVariable int id,
                                                          @RequestBody AdmissionTeacher teacher,
                                                          @RequestParam String role,
                                                          @RequestParam String email) {
        return ResponseEntity.ok(admissionTeacherService.updateTeacher(id, teacher, role, email));
    }

    @DeleteMapping("/deleteTeacher/{id}")
    public ResponseEntity<String> deleteTeacher(@PathVariable int id,
                                                @RequestParam String role,
                                                @RequestParam String email) {
        admissionTeacherService.deleteTeacher(id, role, email);
        return ResponseEntity.ok("Teacher deleted successfully");
    }


    @PostMapping("/teacherlogin")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = admissionTeacherService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/classroomsbyteacheremail")
    public List<AdmissionClassRoom> getClassRoomsAssignedToTeacher(
            @RequestParam String teacherEmail,
            @RequestParam String role,
            @RequestParam String email
    ) {
        return admissionTeacherService.getClassRoomsByTeacherEmail(teacherEmail, role, email);
    }

    @PostMapping("/sendotpteacher")
    public ResponseEntity<String> sendOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(admissionTeacherService.sendOtpToEmail(body.get("email")));
    }

    @PostMapping("/resetpasswordteacher")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(admissionTeacherService.resetPassword(
                body.get("email"),
                body.get("otp"),
                body.get("newPassword")
        ));
    }

    @GetMapping("/studentsbyclass")
    public List<AdmissionForm> getStudentsByTeacherAndClassroom(
            @RequestParam Long classId,
            @RequestParam String role,
            @RequestParam String email) {

        return admissionTeacherService.getStudentsByTeacherAndClassroom(classId, role, email);
    }

    @GetMapping("/classroomcount")
    public long getMyClassroomCount(
            @RequestParam String role,
            @RequestParam String email) {

        return admissionTeacherService.getClassroomCountByTeacherEmail(email, role, email);
    }

    @GetMapping("/AttendanceAdmissionFormId")
    public ResponseEntity<List<AdmissionAttendance>> getAttendanceByAdmissionFormId(
            @RequestParam Long admissionFormId,
            @RequestParam String role,
            @RequestParam String email
    ) {
        List<AdmissionAttendance> attendanceList = admissionTeacherService
                .getAttendanceByAdmissionFormId(admissionFormId, role, email);
        return ResponseEntity.ok(attendanceList);
    }

//    @GetMapping("/classresponse")
//    public List<TeacherAttendanceSummaryDTO> getAttendanceSummary(
//            @RequestParam String email,  // logged-in user email
//            @RequestParam String role    // logged-in user role
//    ) {
//        // teacherEmail = logged-in user email
//        return admissionTeacherService.getAttendanceSummary(email,role);
//    }
}