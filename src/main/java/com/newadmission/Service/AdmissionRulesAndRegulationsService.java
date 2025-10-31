package com.newadmission.Service;

import com.newadmission.Entity.AdmissionRulesAndRegulations;

import java.util.List;

public interface AdmissionRulesAndRegulationsService {
    AdmissionRulesAndRegulations createRule(AdmissionRulesAndRegulations rule, String role, String email);
    List<AdmissionRulesAndRegulations> getAllRules(String role, String email, String branchCode);
    AdmissionRulesAndRegulations updateRule(Long id, AdmissionRulesAndRegulations rule, String role, String email);
    void deleteRule(Long id, String role, String email);
    AdmissionRulesAndRegulations getRuleById(Long id, String role, String email);
}