package com.newadmission.Service;

import com.newadmission.Entity.ClassRoomSubjectDetails;

import java.util.List;

public interface ClassRoomSubjectDetailsService {
    ClassRoomSubjectDetails create(ClassRoomSubjectDetails details, String role, String email);
    List<ClassRoomSubjectDetails> getAll(String role, String email, String branchCode);
    ClassRoomSubjectDetails getById(Long id, String role, String email);
    ClassRoomSubjectDetails update(Long id, ClassRoomSubjectDetails details, String role, String email);
    void delete(Long id, String role, String email);

    Long getIdByTopicName(String topicName, String role, String email);

    List<String> getTopicNamesByFilters(Long classroomId, Long subjectId, Long examTypeId, Long paperTypeId, String role, String email);

}