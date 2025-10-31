package com.newadmission.Service;

import com.newadmission.DTO.SubjectInfoDTO;
import com.newadmission.DTO.TeacherInfoDTO;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;

import java.util.List;

public interface AdmissionClassRoomService {
    AdmissionClassRoom createClassRoom(AdmissionClassRoom classRoom, String role, String email,
                                       Long mediumId, Long courseId,
                                       List<Long> subjectIds, List<Long> teacherIds);

    List<AdmissionClassRoom> getAllClassRooms(String role, String email, String branchCode);
    AdmissionClassRoom updateClassRoom(Long id, AdmissionClassRoom updated, String role, String email,
                                       Long mediumId, Long courseId,
                                       List<Long> subjectIds, List<Long> teacherIds);
    void deleteClassRoom(Long id, String role, String email);
    AdmissionClassRoom getClassRoomById(Long id, String role, String email);

    List<AdmissionForm> assignAdmissionsToClassRoom(Long classRoomId, List<Long> admissionIds,
                                                    String role, String email);

    List<TeacherInfoDTO> getTeachersByClassRoomId(Long classroomId, String role, String email);
    List<SubjectInfoDTO> getSubjectsByClassRoomId(Long classroomId, String role, String email);

}
