package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.StudentSubjectResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSubjectResultRepository extends JpaRepository<StudentSubjectResult, Long> {
    List<StudentSubjectResult> findByStudent(AdmissionForm student);
    List<StudentSubjectResult> findAllByBranchCode(String branchCode);
    List<StudentSubjectResult> findAllByStudentId(Long studentId);

//    List<StudentSubjectResult> findByStudentId(Long studentId);
//
//    List<StudentSubjectResult> findAllByStudentIdAndBranchCode(Long studentId, String branchCode);

    List<StudentSubjectResult> findAll(); // already inherited from JpaRepository
    List<StudentSubjectResult> findByStudentIdAndExamTypeIdAndPaperTypeId(Long studentId, Long examTypeId, Long paperTypeId);

}