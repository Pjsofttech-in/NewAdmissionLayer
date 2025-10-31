package com.newadmission.Repository;

import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionHomework;
import com.newadmission.Entity.AdmissionHomeworkSubmission;
import com.newadmission.Entity.AdmissionTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionHomeworkRepository extends JpaRepository<AdmissionHomework, Long> {
    List<AdmissionHomework> findByClassroom(AdmissionClassRoom classroom);

    List<AdmissionHomework> findByClassroomIdAndGivenByTeacherEmail(Long classroomId, String teacherEmail);

}