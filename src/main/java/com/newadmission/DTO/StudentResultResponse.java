package com.newadmission.DTO;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentResultResponse {
    private Long studentId;
    private String studentName;
    private String email;
    private String academicYear;
    private String mediumName;
    private String coursename;
    private int rollno;
    private String batchName;
    private Double percentage;
    private String status;
    private Integer totalObtainedMarks;
    private int totalSubjectMarks;

    @CreationTimestamp
    private LocalDate resultDate;

    private List<SubjectResultDto> subjectResults;
}