package com.newadmission.Repository;

import com.newadmission.DTO.*;
import com.newadmission.Entity.AdmissionForm;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionRepository extends JpaRepository<AdmissionForm, Long> , JpaSpecificationExecutor<AdmissionForm> {
    List<AdmissionForm> findAllByBranchCode(String branchCode);
    @Query("SELECT a FROM AdmissionForm a WHERE a.date BETWEEN :start AND :end AND a.branchCode IN :branchCodes")
    List<AdmissionForm> findByDateBetweenAndBranchCode(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("branchCodes") List<String> branchCodes
    );


    // Grabs daily revenue directly from the Admission form,
    // BUT ONLY if their installments list is empty (prevents double-counting).
    @Query("SELECT a.paymentDate AS date, SUM(a.paidFees) AS total " +
            "FROM AdmissionForm a " +
            "WHERE a.paymentDate BETWEEN :startDate AND :endDate " +
            "AND a.paidFees > 0 " +
            "AND a.installments IS EMPTY " +
            "AND a.branchCode = :branchCode " +
            "GROUP BY a.paymentDate")
    List<DailyRevenueDTO> getDirectAdmissionRevenueBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    /**
     * Calculates summary for students paying upfront (no installments) based on Admission Due Date.
     */
    @Query("SELECT " +
            "COALESCE(SUM(a.totalFees), 0.0) AS totalFees, " +
            "COALESCE(SUM(a.paidFees), 0.0) AS paidFees, " +
            "COALESCE(SUM(a.pendingFees), 0.0) AS pendingFees " +
            "FROM AdmissionForm a " +
            "WHERE a.dueDate BETWEEN :startDate AND :endDate " +
            "AND a.branchCode = :branchCode " +
            "AND a.installments IS EMPTY")
    FeeSummaryProjection getDirectAdmissionSummaryByMonth(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("SELECT a.paymentMode AS paymentMode, SUM(a.paidFees) AS totalAmount " +
            "FROM AdmissionForm a " +
            "WHERE a.paymentDate BETWEEN :startDate AND :endDate " +
            "AND a.paidFees > 0 " +
            "AND a.installments IS EMPTY " +
            "AND a.branchCode = :branchCode " +
            "GROUP BY a.paymentMode")
    List<PaymentModeSummaryDTO> getDirectAdmissionRevenueByPaymentMode(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("SELECT a.coursename AS courseName, SUM(a.paidFees) AS totalAmount " +
            "FROM AdmissionForm a " +
            "WHERE a.paymentDate BETWEEN :startDate AND :endDate " +
            "AND a.paidFees > 0 " +
            "AND a.installments IS EMPTY " +
            "AND a.branchCode = :branchCode " +
            "GROUP BY a.coursename")
    List<CourseRevenueDTO> getDirectAdmissionRevenueByCourse(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("SELECT YEAR(a.paymentDate) AS year, SUM(a.paidFees) AS totalAmount " +
            "FROM AdmissionForm a " +
            "WHERE a.paymentDate BETWEEN :startDate AND :endDate " +
            "AND a.paidFees > 0 " +
            "AND a.installments IS EMPTY " +
            "AND a.branchCode = :branchCode " +
            "GROUP BY YEAR(a.paymentDate) " +
            "ORDER BY YEAR(a.paymentDate) ASC")
    List<YearlyRevenueDTO> getDirectAdmissionRevenueByYearRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchCode") String branchCode
    );

    @Query("SELECT MONTH(a.paymentDate) AS month, SUM(a.paidFees) AS totalAmount " +
            "FROM AdmissionForm a " +
            "WHERE YEAR(a.paymentDate) = :year " +
            "AND a.paidFees > 0 " +
            "AND a.installments IS EMPTY " +
            "AND a.branchCode = :branchCode " +
            "GROUP BY MONTH(a.paymentDate) " +
            "ORDER BY MONTH(a.paymentDate) ASC")
    List<MonthlyRevenueDTO> getDirectAdmissionRevenueByYearMonthWise(
            @Param("year") int year,
            @Param("branchCode") String branchCode
    );
//
    @Query("SELECT a FROM AdmissionForm a WHERE a.id IN :ids")
    List<AdmissionForm> findAllById(@Param("ids") List<Long> ids);

    @Query("SELECT a FROM AdmissionForm a WHERE a.academicYear = :academicYear AND a.coursename = :courseName AND a.mediumName = :mediumName AND a.branchCode = :branchCode AND a.admissionClassRoom IS NULL")
    List<AdmissionForm> findByAcademicYearAndCoursenameAndMediumNameAndBranchCode(
            String academicYear, String courseName, String mediumName, String branchCode);

    List<AdmissionForm> findByAdmissionClassRoomIdAndBranchCode(Long classroomId, String branchCode);

    List<AdmissionForm> findByAdmissionClassRoomIdInAndBranchCode(List<Long> classroomIds, String branchCode);


    @Query("SELECT MAX(a.rollNo) FROM AdmissionForm a WHERE a.admissionClassRoom.id = :classRoomId")
    Integer findMaxRollNoByClassRoomId(@Param("classRoomId") Long classRoomId);


    AdmissionForm findByRollNoAndAdmissionClassRoomId(Integer rollNo, Long admissionClassRoomId);

    List<AdmissionForm> findByAdmissionClassRoomId(long classroomId);

    // In AdmissionFormRepository.java
    Long countByAdmissionClassRoomIdAndBranchCode(Long classroomId, String branchCode);
    Optional<AdmissionForm> findByEmail(String email);

    Optional<AdmissionForm> findByAdmissionClassRoom_IdAndRollNoAndBranchCode(Long classroomId, Integer rollNo, String branchCode);

    Optional<AdmissionForm> findTopByOrderByIdDesc();

    @Query("SELECT a FROM AdmissionForm a " +
            "JOIN a.admissionClassRoom c " +
            "JOIN c.teachers t " +
            "WHERE t.email = :teacherEmail AND c.id = :classId AND a.branchCode = :branchCode")
    List<AdmissionForm> findByTeacherAndClassroom(String teacherEmail, Long classId, String branchCode);

    List<AdmissionForm> findByBranchCodeInAndDateIsNotNull(List<String> branchCodes);
    List<AdmissionForm> findByBranchCodeInAndDateBetween(List<String> branchCodes, LocalDate startDate, LocalDate endDate);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT MAX(a.rollNo) FROM AdmissionForm a WHERE a.admissionClassRoom.id = :classRoomId")
    Integer findMaxRollNoByClassRoomIdForUpdate(@Param("classRoomId") Long classRoomId);


    @Query("SELECT a FROM AdmissionForm a WHERE a.parentEmail = :parentEmail AND a.parentEmail IS NOT NULL")
    Optional<AdmissionForm> findByParentEmail(@Param("parentEmail") String parentEmail);

}