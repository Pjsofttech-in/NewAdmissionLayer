package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionConductBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionConductByRepository extends JpaRepository<AdmissionConductBy, Long> {

    @Query("SELECT a FROM AdmissionConductBy a WHERE a.branchCode = :branchCode")
    List<AdmissionConductBy> findAllByBranchCode(@Param("branchCode") String branchCode);
}