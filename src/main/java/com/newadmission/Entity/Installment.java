package com.newadmission.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNo;

    @Positive(message = "Amount cannot be negative or zero")
    private double amount;

    private String paidBy;

    private String transactionId;

    private LocalDate installmentDate;

    private LocalDate dueDate;

    private String status;

    private String remark;

    private String  installmentCount;

    private String month;

    @Email
    private String createdByEmail;
    private String role;
    private String branchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id")
    @JsonIgnore
    private AdmissionForm admission;
}