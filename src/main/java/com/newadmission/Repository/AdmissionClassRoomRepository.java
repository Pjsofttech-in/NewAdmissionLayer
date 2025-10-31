package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionClassRoomRepository extends JpaRepository<AdmissionClassRoom, Long> {
    @Query("SELECT a FROM AdmissionClassRoom a WHERE a.branchCode = :branchCode")
    List<AdmissionClassRoom> findAllByBranchCode(@Param("branchCode") String branchCode);

    List<AdmissionClassRoom> findByAcademicYearAndBranchCode(String academicYear, String branchCode);

    @Query("SELECT c FROM AdmissionClassRoom c JOIN c.teachers t WHERE t.email = :email AND c.branchCode = :branchCode")
    List<AdmissionClassRoom> findByTeachersEmailAndBranchCode(@Param("email") String email, @Param("branchCode") String branchCode);


    @Query("SELECT c FROM AdmissionClassRoom c JOIN c.teachers t WHERE LOWER(t.email) = LOWER(:email)")
    List<AdmissionClassRoom> findByTeacherEmail(@Param("email") String email);

    @Query("SELECT COUNT(c) FROM AdmissionClassRoom c JOIN c.teachers t WHERE t.email = :teacherEmail")
    long countByTeacherEmail(String teacherEmail);

    @Query("SELECT t FROM AdmissionClassRoom c JOIN c.teachers t WHERE c.id = :classroomId")
    List<AdmissionTeacher> findTeachersByClassroomId(@Param("classroomId") Long classroomId);
}
