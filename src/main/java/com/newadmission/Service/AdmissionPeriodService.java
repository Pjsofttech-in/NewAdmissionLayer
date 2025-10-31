package com.newadmission.Service;

import com.newadmission.Entity.AdmissionPeriod;

import java.util.List;

public interface AdmissionPeriodService {
    AdmissionPeriod createPeriod(AdmissionPeriod period, String role, String email);
    AdmissionPeriod updatePeriod(Integer id, AdmissionPeriod period, String role, String email);
    List<AdmissionPeriod> getAllPeriods(String role, String email, String branchCode);
    AdmissionPeriod getPeriodById(Integer id, String role, String email);
    void deletePeriod(Integer id, String role, String email);
}