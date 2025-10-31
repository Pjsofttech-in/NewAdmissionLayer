package com.newadmission.Service;

import com.newadmission.Entity.AdmissionTermsAndCondition;

import java.util.List;

public interface AdmissionTermsAndConditionService {
    AdmissionTermsAndCondition createTerm(AdmissionTermsAndCondition term, String role, String email);
    List<AdmissionTermsAndCondition> getAllTerms(String role, String email, String branchCode);
    AdmissionTermsAndCondition updateTerm(Long id, AdmissionTermsAndCondition term, String role, String email);
    void deleteTerm(Long id, String role, String email);
    AdmissionTermsAndCondition getTermById(Long id, String role, String email);
}