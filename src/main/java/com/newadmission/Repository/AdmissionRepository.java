package com.newadmission.Repository;

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