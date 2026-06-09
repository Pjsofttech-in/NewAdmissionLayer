package com.newadmission.DTO;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdmissionFilterRequest {
    private String role;
    private String email;
    private String branchCode;

    // Optional dynamic filters
    private String name;
    private String mobile1;
    private String status;
    private String coursename;
    private String guideName;
    private String sourceBy;
    private String academicYear;
    private String gender;
    private String mediumName; // <-- newly added
    private String paymentMethod;
    private String paymentMode;
    private String reference;
    private String filterType; // today, last7days, last30days, last365days, custom
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate paymentDueDate;

    private String installmentCount; // <-- newly added
    private String month;

    private String createdByEmail;

    private int page = 0;
    private int size = 10;
}