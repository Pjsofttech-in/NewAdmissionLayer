package com.newadmission.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class PeriodAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String status;
    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "period_id")
    private AdmissionPeriod period;

    @ManyToOne
    @JoinColumn(name = "timetable_id")
    @JsonIgnore
    private AdmissionTimetable timetable;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private AdmissionTeacher teacher;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private AdmissionSubject subject;
}