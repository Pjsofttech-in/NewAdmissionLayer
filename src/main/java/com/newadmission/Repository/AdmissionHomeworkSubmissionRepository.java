package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionHomework;
import com.newadmission.Entity.AdmissionHomeworkSubmission;
import com.newadmission.Entity.AdmissionTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionHomeworkSubmissionRepository extends JpaRepository<AdmissionHomeworkSubmission, Long> {
    List<AdmissionHomeworkSubmission> findByHomework(AdmissionHomework homework);

}