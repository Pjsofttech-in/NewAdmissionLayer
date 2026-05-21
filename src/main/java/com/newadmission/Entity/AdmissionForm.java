package com.newadmission.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class AdmissionForm  {
//    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile1;
    private LocalDate date;
    private String coursename;
    private String duration;
    @Email(message = "Email should be valid")
    private String email;
    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile2;
    private String transactionId;
    private Double totalFees;
    private String remark;
    private String status;
    private String qualification;
    private String city;
    private Long pincode;
    private String state;
    private LocalDate dueDate;

    private String mediumName;
    private String paymentMethod;
    private String studentImage;

    private String paymentImage;
    private Double pendingFees;
    private Double paidFees;
//    private String guideName;
    private String sourceBy;
    private String paymentMode;
    private String currentAddress;
    private String permanentAddress;
    private String academicYear;
    @Column(nullable = false)
    private Integer rollNo = 0;
    private LocalDate paymentDate;
    private String gender;
    @Past(message = "Date of Birth must be in the past")
    private LocalDate dob;
    private LocalDate expiredate;
    private String reference;


    @Email
    private String createdByEmail;
    private String role;
    private String branchCode;
    @Transient
    private String createdByName;
    private String password;


    private String aadhaarCardNo;

    @Column(unique = true)
    private String registrationNo;

    @Email(message = "Email should be valid")
    @Column(name = "parent_email")
    private String parentEmail;

    private String admissionPdf;

    private String studentType;
    private String stream;
    private String classType;

    // --- NEW TAX & ACCOUNTING COLUMNS ---
    private Boolean isGstApplicable;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(name = "gst_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    private Boolean isGstInclusive;

    @OneToMany(mappedBy = "admission", cascade = CascadeType.ALL)
    private List<Installment> installments;


    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private AdmissionClassRoom admissionClassRoom;

    @ManyToMany
    @JoinTable(
            name = "admission_rules",
            joinColumns = @JoinColumn(name = "admission_id"),
            inverseJoinColumns = @JoinColumn(name = "rule_id")
    )
    private List<AdmissionRulesAndRegulations> rulesAndRegulationsList;




}