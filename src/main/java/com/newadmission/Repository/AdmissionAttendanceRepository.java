package com.newadmission.Repository;

import com.newadmission.DTO.AttendanceSummary;
import com.newadmission.Entity.AdmissionAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdmissionAttendanceRepository extends JpaRepository<AdmissionAttendance,Integer> {
    Optional<AdmissionAttendance> findByRollnoAndDateAndClassroomId(Integer rollno, LocalDate date, int classroomId);

    boolean existsByRollnoAndDateAndClassroomId(int rollno, LocalDate date, int classroomId);

    @Query("SELECT a FROM AdmissionAttendance a WHERE a.classroomId = :classroomId AND a.branch_code = :branchCode")
    List<AdmissionAttendance> getByClassroomIdAndBranchCode(int classroomId, String branchCode);

    @Query("""
        SELECT 
            a.rollno AS rollno,
            a.studentName AS studentName,
            a.date AS date,
            a.loginTime AS loginTime,
            a.logoutTime AS logoutTime,
            a.loginStatus AS loginStatus
        FROM AdmissionAttendance a
        WHERE a.classroomId = :classroomId AND a.branch_code = :branchCode
        GROUP BY a.rollno, a.studentName, a.date, a.loginTime, a.logoutTime, a.loginStatus
    """)
    List<AttendanceSummary> getAllSummaryByClassroomAndBranch(int classroomId, String branchCode);

    @Query("""
        SELECT 
            f.name AS studentName,
            a.date AS date,
            a.loginTime AS loginTime,
            a.logoutTime AS logoutTime,
            a.loginStatus AS loginStatus,
            f.id AS admissionId
        FROM AdmissionAttendance a
        JOIN AdmissionForm f ON f.rollNo = a.rollno AND f.name = a.studentName AND f.branchCode = a.branch_code
        WHERE f.id = :admissionId AND f.branchCode = :branchCode
        GROUP BY a.date, f.name, f.id, a.loginTime, a.logoutTime, a.loginStatus
        ORDER BY a.date DESC
    """)
    List<AttendanceSummary> getSummaryByAdmissionIdAndBranchCode(Long admissionId, String branchCode);

    @Query("""
        SELECT 
            f.name AS studentName,
            a.date AS date,
            a.loginTime AS loginTime,
            a.logoutTime AS logoutTime,
            a.loginStatus AS loginStatus,
            f.id AS admissionId
        FROM AdmissionAttendance a
        JOIN AdmissionForm f ON f.rollNo = a.rollno AND f.name = a.studentName AND f.branchCode = a.branch_code
        WHERE a.classroomId = :classroomId
        GROUP BY a.date, f.name, f.id, a.loginTime, a.logoutTime, a.loginStatus
        ORDER BY a.date DESC
    """)
    List<AttendanceSummary> getSummaryByClassroomId(int classroomId);

//    @Query("""
//        SELECT a FROM AdmissionAttendance a
//        WHERE a.branch_code = :branchCode
//        AND a.classroomId = :classroomId
//        AND a.date BETWEEN :startDate AND :endDate
//    """)
//    List<AdmissionAttendance> findAttendanceByBranchAndClassroomAndDateRange(
//            String branchCode,
//            int classroomId,
//            LocalDate startDate,
//            LocalDate endDate
//    );

    @Query("SELECT a FROM AdmissionAttendance a WHERE a.branch_code = :branchCode AND a.classroomId = :classroomId AND a.date BETWEEN :startDate AND :endDate")
    List<AdmissionAttendance> findAttendanceByBranchAndClassroomAndDateRange(
            @Param("branchCode") String branchCode,
            @Param("classroomId") int classroomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    @Query("""
        SELECT a FROM AdmissionAttendance a 
        WHERE a.rollno = :rollno 
        AND a.branch_code = :branchCode 
        AND a.date BETWEEN :startDate AND :endDate
    """)
    List<AdmissionAttendance> findByRollnoAndBranchAndDateRange(
            @Param("rollno") int rollno,
            @Param("branchCode") String branchCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    long countByClassroomId(int classroomId);
    long countByClassroomIdAndDate(int classroomId, LocalDate date);
    long countByClassroomIdAndDateBetween(int classroomId, LocalDate start, LocalDate end);

    List<AdmissionAttendance> findByClassroomId(int classroomId);
    List<AdmissionAttendance> findByClassroomIdAndDate(int classroomId, LocalDate date);
    List<AdmissionAttendance> findByClassroomIdAndDateBetween(int classroomId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT a FROM AdmissionAttendance a WHERE a.rollno = :rollno AND a.classroomId = :classroomId AND a.branch_code = :branchCode")
    List<AdmissionAttendance> findAttendance(@Param("rollno") int rollno,
                                             @Param("classroomId") int classroomId,
                                             @Param("branchCode") String branchCode);
}
