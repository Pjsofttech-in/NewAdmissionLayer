package com.newadmission.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String branch_code;
    private int classroomId;
    private int rollno;
    private String studentName;

    private LocalDate date;
    private LocalTime loginTime;    // NEW
    private LocalTime logoutTime;   // NEW


    private String loginStatus;

}