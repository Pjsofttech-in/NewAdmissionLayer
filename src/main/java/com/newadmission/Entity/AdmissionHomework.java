package com.newadmission.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionHomework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String homework;

    private String imageUrl;   // uploaded to S3
    private String subjectName;

    private LocalDate startDate;
    private LocalDate endDate;


    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private AdmissionTeacher givenByTeacher;   // ✅ Who gave homework

    @ManyToOne
    @JoinColumn(name = "classroom_id", nullable = false)
    private AdmissionClassRoom classroom;   // ✅ Which classroom received it

    // List of submissions
    @OneToMany(mappedBy = "homework", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdmissionHomeworkSubmission> submissions = new ArrayList<>();
}