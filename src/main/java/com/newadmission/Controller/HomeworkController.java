package com.newadmission.Controller;

import com.newadmission.DTO.HomeworkResponse;
import com.newadmission.DTO.HomeworkSubmissionResponse;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.AdmissionHomework;
import com.newadmission.Entity.AdmissionHomeworkSubmission;
import com.newadmission.Service.HomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
@RestController
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    // ✅ Assign Homework
    @PostMapping("/assignHomework")
    public ResponseEntity<HomeworkResponse> assignHomework(
            @RequestParam String homeworkText,
            @RequestParam String subject,
            @RequestParam Long classroomId,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam String branchCode,
            @RequestParam String teacherEmail,
            @RequestParam String startDate, // yyyy-MM-dd
            @RequestParam String endDate    // yyyy-MM-dd
    ) throws IOException {
        HomeworkResponse response = homeworkService.assignHomework(
                homeworkText, subject, classroomId, file, branchCode, teacherEmail, startDate, endDate
        );
        return ResponseEntity.ok(response);
    }

    // ✅ Submit Homework
    @PostMapping("/submitHomework")
    public ResponseEntity<HomeworkSubmissionResponse> submitHomework(
            @RequestParam Long homeworkId,
            @RequestParam String studentEmail,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String answerText,
            @RequestParam String branchCode,
            @RequestParam(required = false, defaultValue = "SUBMITTED") String status  // ✅ added status
    ) throws IOException {
        HomeworkSubmissionResponse response = homeworkService.submitHomework(
                homeworkId, studentEmail, file, branchCode, answerText, status
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getHomeworkForStudent")
    public ResponseEntity<List<HomeworkResponse>> getHomeworkForStudent(
            @RequestParam String studentEmail
    ) {
        return ResponseEntity.ok(homeworkService.getHomeworkForStudent(studentEmail));
    }

    @GetMapping("/getSubmissionsForHomework")
    public ResponseEntity<List<HomeworkSubmissionResponse>> getSubmissionsForHomework(
            @RequestParam Long homeworkId,
            @RequestParam String teacherEmail
    ) {
        return ResponseEntity.ok(homeworkService.getSubmissionsForHomework(homeworkId, teacherEmail));
    }

    @GetMapping("/getHomeworkByTeacherAndClass")
    public List<HomeworkResponse> getHomeworkByTeacherAndClass(
            @RequestParam Long classId,
            @RequestParam String teacherEmail) {

        return homeworkService.getHomeworkAssignedByTeacherToClass(classId, teacherEmail);
    }



    // ✅ UPDATE homework
    @PutMapping("/updateHomework/{homeworkId}")
    public ResponseEntity<HomeworkResponse> updateHomework(
            @PathVariable Long homeworkId,
            @RequestParam(required = false) String homeworkText,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam String branchCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam String teacherEmail
    ) throws IOException {
        return ResponseEntity.ok(
                homeworkService.updateHomework(homeworkId, homeworkText, subject, file, branchCode, startDate, endDate, teacherEmail)
        );
    }

    // ✅ DELETE homework
    @DeleteMapping("/deleteHomework/{homeworkId}")
    public ResponseEntity<String> deleteHomework(
            @PathVariable Long homeworkId,
            @RequestParam String teacherEmail
    ) {
        homeworkService.deleteHomework(homeworkId, teacherEmail);
        return ResponseEntity.ok("Homework deleted successfully");
    }

    // ✅ UPDATE submission
    @PutMapping("/updateSubmission/{submissionId}")
    public ResponseEntity<HomeworkSubmissionResponse> updateSubmission(
            @PathVariable Long submissionId,
            @RequestParam String studentEmail,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String answerText,
            @RequestParam String branchCode,
            @RequestParam(required = false) String status
    ) throws IOException {
        return ResponseEntity.ok(
                homeworkService.updateSubmission(submissionId, studentEmail, file, branchCode, answerText, status)
        );
    }

    // ✅ DELETE submission
    @DeleteMapping("/deleteSubmission/{submissionId}")
    public ResponseEntity<String> deleteSubmission(
            @PathVariable Long submissionId,
            @RequestParam String studentEmail
    ) {
        homeworkService.deleteSubmission(submissionId, studentEmail);
        return ResponseEntity.ok("Submission deleted successfully");
    }


}