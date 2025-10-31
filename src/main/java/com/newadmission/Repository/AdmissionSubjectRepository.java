package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionSubjectRepository extends JpaRepository<AdmissionSubject, Integer> {
    @Query("SELECT s FROM AdmissionSubject s WHERE s.branchCode = :branchCode")
    List<AdmissionSubject> findAllByBranchCode(String branchCode);





    List<AdmissionSubject> findAllByIdIn(List<Long> subjectIds);  // Changed this

//    List<AdmissionSubject> findByCreatedByEmailAndBranchCode(String email, String branchCode);

}