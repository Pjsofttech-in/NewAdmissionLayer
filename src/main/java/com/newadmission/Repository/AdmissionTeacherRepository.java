package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionTeacherRepository extends JpaRepository<AdmissionTeacher, Integer> {

    @Query("SELECT t FROM AdmissionTeacher t WHERE t.branchCode = :branchCode")
    List<AdmissionTeacher> findAllByBranchCode(String branchCode);




    List<AdmissionTeacher> findAllByIdIn(List<Long> teacherIds);  // Changed this


    Optional<AdmissionTeacher> findByEmail(String email);

    boolean existsByEmail(String email); // Method to check if teacher exists by email

    Optional<AdmissionTeacher> findByEmailAndBranchCode(String email, String branchCode);


}