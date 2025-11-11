package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionSourceBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionSourceByRepository extends JpaRepository<AdmissionSourceBy, Long> {

    @Query("SELECT a FROM AdmissionSourceBy a WHERE a.branchCode = :branchCode")
    List<AdmissionSourceBy> findAllByBranchCode(@Param("branchCode") String branchCode);

    @Query("SELECT a FROM AdmissionSourceBy a WHERE a.branchCode IN :branchCodes")
    List<AdmissionSourceBy> findAllByBranchCodeIn(@Param("branchCodes") List<String> branchCodes);

}