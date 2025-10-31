package com.newadmission.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionHomeworkSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String answerText; // optional text
    private String submittedFileUrl; // uploaded to S3
    private LocalDateTime submittedAt;

    private String status;


    @ManyToOne
    @JoinColumn(name = "homework_id", nullable = false)
    private AdmissionHomework homework;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private AdmissionForm student;   // ✅ which student submitted

}