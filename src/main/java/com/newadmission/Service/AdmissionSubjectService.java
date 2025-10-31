package com.newadmission.Service;

import com.newadmission.Entity.AdmissionSubject;

import java.util.List;
import java.util.Set;

public interface AdmissionSubjectService {

    AdmissionSubject createSubject(AdmissionSubject subject, String role, String email);

    List<AdmissionSubject> getAllSubjects(String role, String email, String branchCode);

    AdmissionSubject getSubjectById(int id, String role, String email);

    AdmissionSubject updateSubject(int id, AdmissionSubject subject, String role, String email);

    void deleteSubject(int id, String role, String email);
}