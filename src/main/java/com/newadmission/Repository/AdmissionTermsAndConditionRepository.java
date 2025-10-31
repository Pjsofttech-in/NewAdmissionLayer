package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionTermsAndCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionTermsAndConditionRepository extends JpaRepository<AdmissionTermsAndCondition, Long> {
    @Query("SELECT t FROM AdmissionTermsAndCondition t WHERE t.branchCode = :branchCode")
    List<AdmissionTermsAndCondition> findAllByBranchCode(String branchCode);
}