package com.newadmission.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class AdmissionTimetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String weekday;

    private String branchCode;
    private String createdByEmail;
    private String role;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private AdmissionClassRoom classRoom;


    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PeriodAssignment> assignments = new HashSet<>();


}