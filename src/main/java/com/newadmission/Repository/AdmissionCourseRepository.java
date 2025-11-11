package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionCourseRepository extends JpaRepository<AdmissionCourse, Long> {

    @Query("SELECT c FROM AdmissionCourse c WHERE c.branchCode = :branchCode")
    List<AdmissionCourse> findAllByBranchCode(String branchCode);


    @Query("SELECT a FROM AdmissionCourse a WHERE a.branchCode IN :branchCodes")
    List<AdmissionCourse> findAllByBranchCodeIn(@Param("branchCodes") List<String> branchCodes);
}
