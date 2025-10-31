package com.newadmission.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class AdmissionPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer periodNo;
    private String startTime;
    private String endTime;

    private String branchCode;
    private String createdByEmail;
    private String role;

    @OneToMany(mappedBy = "period", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<PeriodAssignment> assignments = new HashSet<>();

}