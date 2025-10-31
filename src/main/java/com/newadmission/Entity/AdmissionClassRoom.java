package com.newadmission.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionClassRoom  {
//    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchName;
    private LocalTime batchStartTime;
    private LocalTime batchEndTime;

    @Column(length = 9)
    private String academicYear;

    @Email
    private String createdByEmail;
    private String role;
    private String branchCode;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdDate;

    @ManyToOne
    @JoinColumn(name = "medium_id")
    private AdmissionMedium medium;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private AdmissionCourse course;

    @ManyToMany
    @JoinTable(
            name = "classroom_subjects",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private List<AdmissionSubject> subjects = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "classroom_teachers",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    private List<AdmissionTeacher> teachers = new ArrayList<>();

}