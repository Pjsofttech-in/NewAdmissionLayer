package com.newadmission.Service;

import com.newadmission.Entity.Installment;

import java.util.List;

public interface InstallmentService {

    Installment addInstallmentToAdmission(Long admissionId, Installment installment, String role, String email);

    List<Installment> getInstallmentsByAdmission(Long admissionId, String role, String email,String branchCode);

    void deleteInstallment(Long installmentId, String role, String email);

    Installment getInstallmentById(Long id, String role, String email, String branchCode);

    Installment updateInstallment(Long id, Installment installment, String role, String email);

    Installment updateInstallmentStatus(Long installmentId, String newStatus, String role, String email);

    List<Installment> getFeesDueInDays(Integer days);
}