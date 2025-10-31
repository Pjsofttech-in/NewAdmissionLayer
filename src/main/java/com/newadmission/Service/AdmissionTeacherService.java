package com.newadmission.Service;

//import com.newadmission.DTO.TeacherAttendanceSummaryDTO;
import com.newadmission.Entity.AdmissionAttendance;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.JWT.LoginRequest;
import com.newadmission.JWT.LoginResponse;

import java.util.List;

public interface AdmissionTeacherService {

    AdmissionTeacher createTeacher(AdmissionTeacher teacher, String role, String email);

    List<AdmissionTeacher> getAllTeachers(String role, String email, String branchCode);

    AdmissionTeacher getTeacherById(int id, String role, String email);

    AdmissionTeacher updateTeacher(int id, AdmissionTeacher teacher, String role, String email);

    void deleteTeacher(int id, String role, String email);

    LoginResponse login(LoginRequest request);

    List<AdmissionClassRoom> getClassRoomsByTeacherEmail(String teacherEmail, String role, String email);

    String sendOtpToEmail(String email);

    String resetPassword(String email, String otp, String newPassword);

    List<AdmissionForm> getStudentsByTeacherAndClassroom(Long classId, String role, String email);

    long getClassroomCountByTeacherEmail(String teacherEmail, String role, String email);

    List<AdmissionAttendance> getAttendanceByAdmissionFormId(Long admissionFormId, String role, String email);

//    List<TeacherAttendanceSummaryDTO> getAttendanceSummary(String email, String role);

}