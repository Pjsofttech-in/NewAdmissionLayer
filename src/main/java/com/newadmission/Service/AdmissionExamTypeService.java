package com.newadmission.Service;

import com.newadmission.Entity.AdmissionExamType;

import java.util.List;

public interface AdmissionExamTypeService {
    AdmissionExamType createExamType(AdmissionExamType examType, String role, String email);
    List<AdmissionExamType> getAllExamTypes(String role, String email, String branchCode);
    AdmissionExamType getExamTypeById(Long id, String role, String email);
    AdmissionExamType updateExamType(Long id, AdmissionExamType examType, String role, String email);
    void deleteExamType(Long id, String role, String email);

    List<AdmissionExamType> getExamTypesByTeacherEmailAndBranchCode(String teacherEmail, String branchCode);

}