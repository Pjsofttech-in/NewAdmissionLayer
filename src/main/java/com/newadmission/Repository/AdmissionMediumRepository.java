package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionMedium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionMediumRepository extends JpaRepository<AdmissionMedium, Long> {

    @Query("SELECT a FROM AdmissionMedium a WHERE a.branchCode = :branchCode")
    List<AdmissionMedium> findAllByBranchCode(@Param("branchCode") String branchCode);
}