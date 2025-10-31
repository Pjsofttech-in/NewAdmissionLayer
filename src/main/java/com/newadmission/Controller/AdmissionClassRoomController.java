package com.newadmission.Controller;

import com.newadmission.DTO.SubjectInfoDTO;
import com.newadmission.DTO.TeacherInfoDTO;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Service.AdmissionClassRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionClassRoomController {

    @Autowired
    private AdmissionClassRoomService classRoomService;

    @PostMapping("/createClassRoom")
    public ResponseEntity<AdmissionClassRoom> createClassRoom(@RequestBody AdmissionClassRoom classRoom,
                                                              @RequestParam String role,
                                                              @RequestParam String email,
                                                              @RequestParam Long mediumId,
                                                              @RequestParam Long courseId,
                                                              @RequestParam List<Long> subjectIds,
                                                              @RequestParam List<Long> teacherIds) {
        return ResponseEntity.ok(classRoomService.createClassRoom(
                classRoom, role, email, mediumId, courseId, subjectIds, teacherIds));
    }


    @GetMapping("/getAllClassRooms")
    public ResponseEntity<List<AdmissionClassRoom>> getAllClassRooms(@RequestParam String role,
                                                                     @RequestParam String email,
                                                                     @RequestParam String branchCode) {
        return ResponseEntity.ok(classRoomService.getAllClassRooms(role, email, branchCode));
    }

    @GetMapping("/getClassRoomById/{id}")
    public ResponseEntity<AdmissionClassRoom> getClassRoomById(@PathVariable Long id,
                                                               @RequestParam String role,
                                                               @RequestParam String email) {
        return ResponseEntity.ok(classRoomService.getClassRoomById(id, role, email));
    }

    @PutMapping("/updateClassRoom/{id}")
    public ResponseEntity<AdmissionClassRoom> updateClassRoom(@PathVariable Long id,
                                                              @RequestBody AdmissionClassRoom classRoom,
                                                              @RequestParam String role,
                                                              @RequestParam String email,
                                                              @RequestParam(required = false) Long mediumId,
                                                              @RequestParam(required = false) Long courseId,
                                                              @RequestParam(required = false) List<Long> subjectIds,
                                                              @RequestParam(required = false) List<Long> teacherIds) {
        AdmissionClassRoom updated = classRoomService.updateClassRoom(id, classRoom, role, email,
                mediumId, courseId, subjectIds, teacherIds);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/deleteClassRoom/{id}")
    public ResponseEntity<String> deleteClassRoom(@PathVariable Long id,
                                                  @RequestParam String role,
                                                  @RequestParam String email) {
        classRoomService.deleteClassRoom(id, role, email);
        return ResponseEntity.ok("Class Room deleted successfully");
    }

    @PostMapping("/assignAdmissionsToClassRoom")
    public ResponseEntity<List<AdmissionForm>> assignAdmissionsToClassRoom(
            @RequestParam Long classRoomId,
            @RequestParam List<Long> admissionIds,
            @RequestParam String role,
            @RequestParam String email) {
        List<AdmissionForm> updatedAdmissions = classRoomService.assignAdmissionsToClassRoom(
                classRoomId, admissionIds, role, email);
        return ResponseEntity.ok(updatedAdmissions);
    }


    @GetMapping("/getTeachersByClassRoomId")
    public List<TeacherInfoDTO> getTeachers(
            @RequestParam Long classroomId,
            @RequestParam String role,
            @RequestParam String email) {

        return classRoomService.getTeachersByClassRoomId(classroomId, role, email);
    }

    @GetMapping("/getSubjectsByClassRoomId")
    public List<SubjectInfoDTO> getSubjects(
            @RequestParam Long classroomId,
            @RequestParam String role,
            @RequestParam String email) {

        return classRoomService.getSubjectsByClassRoomId(classroomId, role, email);
    }
}
