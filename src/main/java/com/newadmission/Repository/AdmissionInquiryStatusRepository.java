package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionInquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionInquiryStatusRepository extends JpaRepository<AdmissionInquiryStatus, Long> {
    List<AdmissionInquiryStatus> findAllByBranchCode(String branchCode);
}