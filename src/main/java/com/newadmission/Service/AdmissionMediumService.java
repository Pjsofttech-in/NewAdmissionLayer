package com.newadmission.Service;

import com.newadmission.Entity.AdmissionMedium;

import java.util.List;

public interface AdmissionMediumService {

    AdmissionMedium createMedium(AdmissionMedium medium, String role, String email);

    List<AdmissionMedium> getAllMediums(String role, String email,String branchCode);

    AdmissionMedium updateMedium(Long id, AdmissionMedium medium, String role, String email);

    void deleteMedium(Long id, String role, String email);

    AdmissionMedium getMediumById(Long id, String role, String email);

}