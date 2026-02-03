package com.newadmission.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class AdmissionPaymentTransaction
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long admissionId;

    @Column(unique = true,length = 50)
    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Column(length = 255)
    private String razorpaySignature;

    private Long amount;

    private String status;

    private String branchCode;
    private String systemName;

    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
