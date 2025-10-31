package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionExamTypeRepository extends JpaRepository<AdmissionExamType, Long> {
    @Query("SELECT e FROM AdmissionExamType e WHERE e.branchCode = :branchCode")
    List<AdmissionExamType> findAllByBranchCode(@Param("branchCode") String branchCode);
}