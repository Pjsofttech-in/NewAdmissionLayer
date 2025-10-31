package com.newadmission.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubjectResult  {
//    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int obtainedMarks;

    private Double percentage;
    private String status;


    private Integer totalObtainedMarks;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private AdmissionForm student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_details_id", nullable = false)
    private ClassRoomSubjectDetails subjectDetails;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_type_id", nullable = false)
    private AdmissionExamType examType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paper_type_id", nullable = false)
    private AdmissionPaperType paperType;

    @Email
    private String createdByEmail;
    private String role;
    private String branchCode;

    @CreationTimestamp
    private LocalDate resultDate;

}