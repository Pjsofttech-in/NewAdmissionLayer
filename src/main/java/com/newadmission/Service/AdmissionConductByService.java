package com.newadmission.Service;

import com.newadmission.Entity.AdmissionConductBy;

import java.util.List;

public interface AdmissionConductByService {
    AdmissionConductBy createConductBy(AdmissionConductBy conductBy, String role, String email);
    List<AdmissionConductBy> getAllConductBy(String role, String email, String branchCode);
    AdmissionConductBy updateConductBy(Long id, AdmissionConductBy conductBy, String role, String email);
    void deleteConductBy(Long id, String role, String email);
    AdmissionConductBy getConductById(Long id, String role, String email);
}