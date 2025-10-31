package com.newadmission.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class AdmissionRulesAndRegulations {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 1500)
    private String rulesAndRegulations;
    @Email
    private String createdByEmail;
    private String role;
    private String branchCode;
}