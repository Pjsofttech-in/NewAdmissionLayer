package com.newadmission.Service;

import com.newadmission.Entity.AdmissionExamType;
import com.newadmission.Entity.AdmissionPaperType;

import java.util.List;

public interface AdmissionPaperTypeService {
    AdmissionPaperType createPaperType(AdmissionPaperType paperType, String role, String email);
    List<AdmissionPaperType> getAll(String role, String email, String branchCode);
    AdmissionPaperType getById(Long id, String role, String email);
    AdmissionPaperType update(Long id, AdmissionPaperType paperType, String role, String email);
    void delete(Long id, String role, String email);
    List<AdmissionPaperType> getPaperTypesByTeacherEmailAndBranchCode(String teacherEmail, String branchCode);

}