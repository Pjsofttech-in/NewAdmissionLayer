package com.newadmission.Repository;

import com.newadmission.DTO.*;
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

    @Query("SELECT i.installmentDate AS date, SUM(i.amount) AS total " +
            "FROM Installment i " +
            "WHERE i.installmentDate BETWEEN :startDate AND :endDate " +
            "AND UPPER(i.status) = 'PAID' " +
            "AND i.branchCode = :branchCode " +
            "GROUP BY i.installmentDate " +
            "ORDER BY i.installmentDate ASC")
    List<DailyRevenueDTO> getDailyRevenueBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    /**
     * Calculates summary for students on an installment plan based on Due Date.
     */
    @Query("SELECT " +
            "COALESCE(SUM(i.amount), 0.0) AS totalFees, " +
            "COALESCE(SUM(CASE WHEN UPPER(i.status) = 'PAID' THEN i.amount ELSE 0.0 END), 0.0) AS paidFees, " +
            "COALESCE(SUM(CASE WHEN UPPER(i.status) = 'PENDING' THEN i.amount ELSE 0.0 END), 0.0) AS pendingFees " +
            "FROM Installment i " +
            "WHERE i.dueDate BETWEEN :startDate AND :endDate " +
            "AND i.branchCode = :branchCode")
    FeeSummaryProjection getInstallmentSummaryByMonth(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("SELECT i.paidBy AS paymentMode, SUM(i.amount) AS totalAmount " +
            "FROM Installment i " +
            "WHERE i.installmentDate BETWEEN :startDate AND :endDate " +
            "AND UPPER(i.status) = 'PAID' " +
            "AND i.branchCode = :branchCode " +
            "GROUP BY i.paidBy")
    List<PaymentModeSummaryDTO> getInstallmentRevenueByPaymentMode(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("SELECT YEAR(i.installmentDate) AS year, SUM(i.amount) AS totalAmount " +
            "FROM Installment i " +
            "WHERE i.installmentDate BETWEEN :startDate AND :endDate " +
            "AND UPPER(i.status) = 'PAID' " +
            "AND i.branchCode = :branchCode " +
            "GROUP BY YEAR(i.installmentDate) " +
            "ORDER BY YEAR(i.installmentDate) ASC")
    List<YearlyRevenueDTO> getInstallmentRevenueByYearRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("SELECT a.coursename AS courseName, SUM(i.amount) AS totalAmount " +
            "FROM Installment i JOIN i.admission a " +
            "WHERE i.installmentDate BETWEEN :startDate AND :endDate " +
            "AND UPPER(i.status) = 'PAID' " +
            "AND i.branchCode = :branchCode " +
            "GROUP BY a.coursename")
    List<CourseRevenueDTO> getInstallmentRevenueByCourse(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("""
            SELECT i FROM Installment i LEFT JOIN FETCH i.admission a
            WHERE lower(i.status) = 'pending' AND i.dueDate BETWEEN :low AND :high
            ORDER BY i.id DESC
            """)
    List<Installment> getAllScheduledFeesDueInBetween(@Param("low") LocalDate low, @Param("high") LocalDate high);

    @Query("SELECT MONTH(i.installmentDate) AS month, SUM(i.amount) AS totalAmount " +
            "FROM Installment i " +
            "WHERE YEAR(i.installmentDate) = :year " +
            "AND UPPER(i.status) = 'PAID' " +
            "AND i.branchCode = :branchCode " +
            "GROUP BY MONTH(i.installmentDate) " +
            "ORDER BY MONTH(i.installmentDate) ASC")
    List<MonthlyRevenueDTO> getInstallmentRevenueByYearMonthWise(
            @Param("year") int year,
            @Param("branchCode") String branchCode
    );
}