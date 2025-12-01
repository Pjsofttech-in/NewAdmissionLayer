package com.newadmission.Service;

import com.newadmission.DTO.StudentResultFilterRequest;
import com.newadmission.DTO.StudentResultResponse;
import com.newadmission.Entity.StudentSubjectResult;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface StudentSubjectResultService {
    List<StudentSubjectResult> createMultiple(List<StudentSubjectResult> results, String role, String email);
    List<StudentSubjectResult> getAll(String role, String email, String branchCode);
    StudentSubjectResult getById(Long id, String role, String email);
    StudentSubjectResult update(Long id, StudentSubjectResult result, String role, String email);
    void delete(Long id, String role, String email);
    public List<StudentSubjectResult> getByStudentId(Long studentId, String role, String email);
    StudentResultResponse getStudentResultsByStudentId(Long studentId, String role, String email);
    Page<StudentResultResponse> getAllStudentResults(String role, String email, String branchCode,
                                                     StudentResultFilterRequest filter, int page, int size);
    List<Map<String, Object>> getPassFailCounts(String role, String email, String examType, String paperType);

    StudentResultResponse getStudentResultsByStudentId(Long studentId);

}