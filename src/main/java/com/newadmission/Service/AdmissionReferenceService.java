package com.newadmission.Service;

import com.newadmission.Entity.AdmissionReference;

import java.util.List;

public interface AdmissionReferenceService {
    AdmissionReference createReference(AdmissionReference reference, String role, String email);
    List<AdmissionReference> getAllReferences(String role, String email, String branchCode);
    AdmissionReference updateReference(Long id, AdmissionReference reference, String role, String email);
    void deleteReference(Long id, String role, String email);
    AdmissionReference getReferenceById(Long id, String role, String email);
}