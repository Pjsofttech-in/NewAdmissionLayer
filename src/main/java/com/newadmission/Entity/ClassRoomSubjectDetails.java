package com.newadmission.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"topicName", "branchCode"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassRoomSubjectDetails  {
//    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int totalMarks;
    private int passingMarks;

    private int totalSubjectMarks;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", nullable = false)
    private AdmissionClassRoom classroom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private AdmissionSubject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_type_id", nullable = false)
    private AdmissionExamType examType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paper_type_id", nullable = false)
    private AdmissionPaperType paperType;

    private String topicName;

    @Email
    private String createdByEmail;
    private String role;
    private String branchCode;
}