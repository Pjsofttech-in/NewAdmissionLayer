package com.newadmission.Repository;

import com.newadmission.Entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomSubjectDetailsRepository extends JpaRepository<ClassRoomSubjectDetails, Long> {
    List<ClassRoomSubjectDetails> findAllByBranchCode(String branchCode);
    List<ClassRoomSubjectDetails> findByClassroomAndSubjectAndExamTypeAndPaperType(
            AdmissionClassRoom classroom,
            AdmissionSubject subject,
            AdmissionExamType examType,
            AdmissionPaperType paperType);

    Optional<ClassRoomSubjectDetails> findByTopicName(String topicName);


    @Query("SELECT c.topicName FROM ClassRoomSubjectDetails c " +
            "WHERE c.classroom.id = :classroomId " +
            "AND c.subject.id = :subjectId " +
            "AND c.examType.id = :examTypeId " +
            "AND c.paperType.id = :paperTypeId " +
            "AND c.branchCode = :branchCode")
    List<String> findTopicNamesByAllIdsAndBranchCode(
            Long classroomId,
            Long subjectId,
            Long examTypeId,
            Long paperTypeId,
            String branchCode
    );

    Optional<ClassRoomSubjectDetails> findByTopicNameAndBranchCode(String topicName, String branchCode);

}