package com.newadmission.Service;

import com.newadmission.DTO.HomeworkResponse;
import com.newadmission.DTO.HomeworkSubmissionResponse;
import com.newadmission.Entity.*;
import com.newadmission.Repository.*;
import com.newadmission.Serviceimpl.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final AdmissionHomeworkRepository homeworkRepo;
    private final AdmissionHomeworkSubmissionRepository submissionRepo;
    private final AdmissionTeacherRepository teacherRepo;
    private final AdmissionClassRoomRepository classroomRepo;
    private final AdmissionRepository studentRepo;
    private final S3Service s3Service;

    // Assign homework
    public HomeworkResponse assignHomework(String homeworkText, String subject, Long classroomId,
                                           MultipartFile file, String branchCode, String teacherEmail,
                                           String startDateStr, String endDateStr) throws IOException {

        AdmissionTeacher teacher = teacherRepo.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        AdmissionClassRoom classroom = classroomRepo.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = s3Service.uploadFileToDocs(file, branchCode, "admission-sys");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr, formatter) : null;
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr, formatter) : null;

        AdmissionHomework homework = AdmissionHomework.builder()
                .homework(homeworkText)
                .subjectName(subject)
                .givenByTeacher(teacher)
                .classroom(classroom)
                .imageUrl(fileUrl)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        homework = homeworkRepo.save(homework);

        return HomeworkResponse.builder()
                .id(homework.getId())
                .homework(homework.getHomework())
                .subjectName(homework.getSubjectName())
                .imageUrl(homework.getImageUrl())
                .startDate(homework.getStartDate())
                .endDate(homework.getEndDate())
                .teacherName(teacher.getTeacherName())
                .teacherEmail(teacher.getEmail())
                .branchCode(teacher.getBranchCode())
                .build();
    }

    // Submit homework
    public HomeworkSubmissionResponse submitHomework(Long homeworkId, String studentEmail,
                                                     MultipartFile file, String branchCode,
                                                     String answerText,String status) throws IOException {

        AdmissionHomework homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        AdmissionForm student = studentRepo.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = s3Service.uploadFileToDocs(file, branchCode, "admission-sys");
        }

        AdmissionHomeworkSubmission submission = AdmissionHomeworkSubmission.builder()
                .homework(homework)
                .student(student)
                .answerText(answerText)
                .submittedFileUrl(fileUrl)
                .submittedAt(LocalDateTime.now())
                .status(status != null ? status : "SUBMITTED")  // ✅ take student-provided status
                .build();

        submission = submissionRepo.save(submission);

        return HomeworkSubmissionResponse.builder()
                .id(submission.getId())
                .homeworkId(homework.getId())
                .homeworkText(homework.getHomework())
                .studentName(student.getName())
                .studentEmail(student.getEmail())
                .answerText(submission.getAnswerText())
                .submittedFileUrl(submission.getSubmittedFileUrl())
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus())  // ✅ return status
                .build();
    }

    public List<HomeworkResponse> getHomeworkForStudent(String studentEmail) {
        AdmissionForm student = studentRepo.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<AdmissionHomework> homeworks = homeworkRepo.findByClassroom(student.getAdmissionClassRoom());


        return homeworks.stream().map(hw -> HomeworkResponse.builder()
                .id(hw.getId())
                .homework(hw.getHomework())
                .subjectName(hw.getSubjectName())
                .imageUrl(hw.getImageUrl())
                .startDate(hw.getStartDate())
                .endDate(hw.getEndDate())
                .teacherName(hw.getGivenByTeacher().getTeacherName())
                .teacherEmail(hw.getGivenByTeacher().getEmail())
                .branchCode(hw.getGivenByTeacher().getBranchCode())

                .build()
        ).toList();
    }

    public List<HomeworkSubmissionResponse> getSubmissionsForHomework(Long homeworkId, String teacherEmail) {
        AdmissionHomework homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        // Ensure teacher is the one who assigned this homework
        if (!homework.getGivenByTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Not authorized to view this homework submissions");
        }

        List<AdmissionHomeworkSubmission> submissions = submissionRepo.findByHomework(homework);

        return submissions.stream().map(sub -> HomeworkSubmissionResponse.builder()
                .id(sub.getId())
                .homeworkId(homework.getId())
                .homeworkText(homework.getHomework())
                .studentName(sub.getStudent().getName())
                .studentEmail(sub.getStudent().getEmail())
                .answerText(sub.getAnswerText())
                .submittedFileUrl(sub.getSubmittedFileUrl())
                .submittedAt(sub.getSubmittedAt())
                .status(sub.getStatus())  // ✅ add status here
                .build()
        ).toList();
    }

    public List<HomeworkResponse> getHomeworkAssignedByTeacherToClass(Long classroomId, String teacherEmail) {

        List<AdmissionHomework> homeworks = homeworkRepo.findByClassroomIdAndGivenByTeacherEmail(classroomId, teacherEmail);

        return homeworks.stream().map(hw -> HomeworkResponse.builder()
                .id(hw.getId())
                .homework(hw.getHomework())
                .subjectName(hw.getSubjectName())
                .imageUrl(hw.getImageUrl())
                .startDate(hw.getStartDate())
                .endDate(hw.getEndDate())
                .teacherName(hw.getGivenByTeacher().getTeacherName())
                .teacherEmail(hw.getGivenByTeacher().getEmail())
                .branchCode(hw.getGivenByTeacher().getBranchCode())
                .build()
        ).toList();
    }



    // UPDATE homework (teacher)
    public HomeworkResponse updateHomework(Long homeworkId, String homeworkText, String subject,
                                           MultipartFile file, String branchCode,
                                           String startDateStr, String endDateStr, String teacherEmail) throws IOException {

        AdmissionHomework homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        if (!homework.getGivenByTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("You are not authorized to update this homework");
        }

        if (homeworkText != null) homework.setHomework(homeworkText);
        if (subject != null) homework.setSubjectName(subject);

        if (file != null && !file.isEmpty()) {
            String fileUrl = s3Service.uploadFileToDocs(file, branchCode, "admission-sys");
            homework.setImageUrl(fileUrl);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (startDateStr != null) homework.setStartDate(LocalDate.parse(startDateStr, formatter));
        if (endDateStr != null) homework.setEndDate(LocalDate.parse(endDateStr, formatter));

        homework = homeworkRepo.save(homework);

        return HomeworkResponse.builder()
                .id(homework.getId())
                .homework(homework.getHomework())
                .subjectName(homework.getSubjectName())
                .imageUrl(homework.getImageUrl())
                .startDate(homework.getStartDate())
                .endDate(homework.getEndDate())
                .teacherName(homework.getGivenByTeacher().getTeacherName())
                .teacherEmail(homework.getGivenByTeacher().getEmail())
                .branchCode(homework.getGivenByTeacher().getBranchCode())
                .build();
    }

    // DELETE homework (teacher)
    public void deleteHomework(Long homeworkId, String teacherEmail) {
        AdmissionHomework homework = homeworkRepo.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("Homework not found"));

        if (!homework.getGivenByTeacher().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("You are not authorized to delete this homework");
        }

        homeworkRepo.delete(homework);
    }

    // UPDATE submission (student)
    public HomeworkSubmissionResponse updateSubmission(Long submissionId, String studentEmail,
                                                       MultipartFile file, String branchCode,
                                                       String answerText, String status) throws IOException {

        AdmissionHomeworkSubmission submission = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (!submission.getStudent().getEmail().equals(studentEmail)) {
            throw new RuntimeException("You are not authorized to update this submission");
        }

        if (answerText != null) submission.setAnswerText(answerText);
        if (status != null) submission.setStatus(status);

        if (file != null && !file.isEmpty()) {
            String fileUrl = s3Service.uploadFileToDocs(file, branchCode, "admission-sys");
            submission.setSubmittedFileUrl(fileUrl);
        }

        submission = submissionRepo.save(submission);

        return HomeworkSubmissionResponse.builder()
                .id(submission.getId())
                .homeworkId(submission.getHomework().getId())
                .homeworkText(submission.getHomework().getHomework())
                .studentName(submission.getStudent().getName())
                .studentEmail(submission.getStudent().getEmail())
                .answerText(submission.getAnswerText())
                .submittedFileUrl(submission.getSubmittedFileUrl())
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus())
                .build();
    }

    // DELETE submission (student)
    public void deleteSubmission(Long submissionId, String studentEmail) {
        AdmissionHomeworkSubmission submission = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if (!submission.getStudent().getEmail().equals(studentEmail)) {
            throw new RuntimeException("You are not authorized to delete this submission");
        }

        submissionRepo.delete(submission);
    }

}