package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionReferenceRepository extends JpaRepository<AdmissionReference, Long> {

    @Query("SELECT r FROM AdmissionReference r WHERE r.branchCode = :branchCode")
    List<AdmissionReference> findAllByBranchCode(String branchCode);
}