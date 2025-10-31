package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionRulesAndRegulations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionRulesAndRegulationsRepository extends JpaRepository<AdmissionRulesAndRegulations, Long> {

    @Query("SELECT r FROM AdmissionRulesAndRegulations r WHERE r.branchCode = :branchCode")
    List<AdmissionRulesAndRegulations> findAllByBranchCode(String branchCode);
}