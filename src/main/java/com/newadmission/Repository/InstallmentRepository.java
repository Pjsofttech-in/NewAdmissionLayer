package com.newadmission.Repository;

import com.newadmission.Entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByAdmissionId(Long admissionId);

//    boolean existsByInvoiceNo(String invoiceNo);

    List<Installment> findByAdmissionIdAndBranchCode(Long admissionId, String branchCode);

    Optional<Installment> findByIdAndBranchCode(Long id, String branchCode);

    @Query("SELECT i.invoiceNo FROM Installment i ORDER BY i.id DESC LIMIT 1")
    String findLatestInvoiceNo();

//    @Query("SELECT SUM(i.amount) FROM Installment i " +
//            "WHERE (i.status IS NULL OR UPPER(i.status) <> 'PAID') " +
//            "AND i.dueDate <= :today " +
//            "AND i.admission.branchCode = :branchCode")
//    Double getCurrentPending(@Param("today") LocalDate today, @Param("branchCode") String branchCode);
//
//    @Query("SELECT SUM(i.amount) FROM Installment i " +
//            "WHERE (i.status IS NULL OR UPPER(i.status) <> 'PAID') " +
//            "AND i.dueDate > :today " +
//            "AND i.admission.branchCode = :branchCode")
//    Double getFuturePending(@Param("today") LocalDate today, @Param("branchCode") String branchCode);



}