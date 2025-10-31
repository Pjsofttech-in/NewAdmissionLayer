package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionPaperType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionPaperTypeRepository extends JpaRepository<AdmissionPaperType, Long> {
    @Query("SELECT p FROM AdmissionPaperType p WHERE p.branchCode = :branchCode")
    List<AdmissionPaperType> findAllByBranchCode(@Param("branchCode") String branchCode);
}