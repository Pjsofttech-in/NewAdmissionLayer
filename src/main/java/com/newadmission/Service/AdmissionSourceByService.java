package com.newadmission.Service;

import com.newadmission.Entity.AdmissionSourceBy;

import java.util.List;

public interface AdmissionSourceByService {

    AdmissionSourceBy createSourceBy(AdmissionSourceBy sourceBy, String role, String email);
    List<AdmissionSourceBy> getAllSourceBy(String role, String email, String branchCode);
    AdmissionSourceBy updateSourceBy(Long id, AdmissionSourceBy updated, String role, String email);
    void deleteSourceBy(Long id, String role, String email);
    AdmissionSourceBy getSourceById(Long id, String role, String email);

}