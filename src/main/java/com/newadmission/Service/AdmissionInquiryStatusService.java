package com.newadmission.Service;

import com.newadmission.Entity.AdmissionInquiryStatus;

import java.util.List;

public interface AdmissionInquiryStatusService {
    AdmissionInquiryStatus createStatus(AdmissionInquiryStatus status, String role, String email);
    List<AdmissionInquiryStatus> getAllStatuses(String role, String email, String branchCode);
    AdmissionInquiryStatus getStatusById(Long id, String role, String email);
    AdmissionInquiryStatus updateStatus(Long id, AdmissionInquiryStatus updated, String role, String email);
    void deleteStatus(Long id, String role, String email);
}