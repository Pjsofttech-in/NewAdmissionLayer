package com.newadmission.Serviceimpl;

import com.newadmission.DTO.SubjectInfoDTO;
import com.newadmission.DTO.TeacherInfoDTO;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.AdmissionSubject;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.Repository.*;
import com.newadmission.Service.AdmissionClassRoomService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdmissionClassRoomServiceImpl implements AdmissionClassRoomService {

    @Autowired
    private AdmissionClassRoomRepository classRoomRepository;

    @Autowired
    private AdmissionRepository admissionRepository;

    @Autowired
    private WebClient webClient;

    @Autowired
    private StaffService staffService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private AdmissionMediumRepository mediumRepository;

    @Autowired
    private AdmissionCourseRepository courseRepository;

    @Autowired
    private AdmissionSubjectRepository subjectRepository;

    @Autowired
    private AdmissionTeacherRepository teacherRepository;

    @Override
    public AdmissionClassRoom createClassRoom(AdmissionClassRoom classRoom, String role, String email,
                                              Long mediumId, Long courseId,
                                              List<Long> subjectIds, List<Long> teacherIds) {
        if (!staffService.hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to create class room");
        }

        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        classRoom.setRole(role);
        classRoom.setCreatedByEmail(email);
        classRoom.setBranchCode(branchCode);

        classRoom.setMedium(mediumRepository.findById(mediumId)
                .orElseThrow(() -> new RuntimeException("Medium not found")));
        classRoom.setCourse(courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found")));

        // Corrected to use findAllByIdIn
        List<AdmissionSubject> subjects = subjectRepository.findAllByIdIn(subjectIds);  // Corrected this
        List<AdmissionTeacher> teachers = teacherRepository.findAllByIdIn(teacherIds);  // Corrected this

        classRoom.setSubjects(subjects);
        classRoom.setTeachers(teachers);

        return classRoomRepository.save(classRoom);
    }




    @Override
    public List<AdmissionClassRoom> getAllClassRooms(String role, String email, String branchCode) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view class rooms");
        }

        return classRoomRepository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionClassRoom updateClassRoom(Long id, AdmissionClassRoom updated, String role, String email,
                                              Long mediumId, Long courseId,
                                              List<Long> subjectIds, List<Long> teacherIds) {

        if (!staffService.hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to update class room");
        }

        AdmissionClassRoom existing = classRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class room not found"));

        // Only update fields if new values are provided (non-null)
        if (updated.getBatchName() != null)
            existing.setBatchName(updated.getBatchName());

        if (updated.getBatchStartTime() != null)
            existing.setBatchStartTime(updated.getBatchStartTime());

        if (updated.getBatchEndTime() != null)
            existing.setBatchEndTime(updated.getBatchEndTime());

        if (updated.getAcademicYear() != null)
            existing.setAcademicYear(updated.getAcademicYear());

        // Only update relationships if new IDs are provided
        if (mediumId != null)
            existing.setMedium(mediumRepository.findById(mediumId)
                    .orElseThrow(() -> new RuntimeException("Medium not found")));

        if (courseId != null)
            existing.setCourse(courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found")));

        if (subjectIds != null && !subjectIds.isEmpty())
            existing.setSubjects(subjectRepository.findAllByIdIn(subjectIds));

        if (teacherIds != null && !teacherIds.isEmpty())
            existing.setTeachers(teacherRepository.findAllByIdIn(teacherIds));

        return classRoomRepository.save(existing);
    }



    @Override
    public void deleteClassRoom(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("You do not have permission to delete class room");
        }

        classRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class room not found"));

        classRoomRepository.deleteById(id);
    }

    @Override
    public AdmissionClassRoom getClassRoomById(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view class room by ID");
        }

        return classRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class room not found with id: " + id));
    }


    @Transactional
    @Override
    public List<AdmissionForm> assignAdmissionsToClassRoom(Long classRoomId, List<Long> admissionIds,
                                                           String role, String email) {
        if (!staffService.hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You do not have permission to assign admissions to the class room");
        }

        AdmissionClassRoom classRoom = classRoomRepository.findById(classRoomId)
                .orElseThrow(() -> new RuntimeException("Class room not found"));

        // Pessimistic lock घेऊन max rollNo fetch कर
        Integer maxRollNo = admissionRepository.findMaxRollNoByClassRoomIdForUpdate(classRoomId);
        int rollNo = (maxRollNo != null) ? maxRollNo + 1 : 1;

        List<AdmissionForm> admissions = admissionRepository.findAllById(admissionIds);

        String systemName = "admission-sys";

        for (AdmissionForm admission : admissions) {
            if (admission.getAdmissionClassRoom() == null ||
                    !admission.getAdmissionClassRoom().getId().equals(classRoomId)) {

//                if (admission.getRollNo() == null || admission.getRollNo() == 0) {
                admission.setRollNo(rollNo++);
//                }
            }

            admission.setAdmissionClassRoom(classRoom);

            String updatedImageUrl = s3Service.copyImageToAttendanceFolderWithRollNoFilename(
                    admission.getId(),
                    classRoomId,
                    admission.getRollNo(),
                    systemName
            );
            admission.setStudentImage(updatedImageUrl);
        }

        return admissionRepository.saveAll(admissions);
    }

    @Override
    public List<TeacherInfoDTO> getTeachersByClassRoomId(Long classroomId, String role, String email) {
        // Check permission
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view teachers");
        }

        // Fetch classroom
        AdmissionClassRoom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        // Fetch branch code for user
        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        // Branch-level access
        if (!"ADMIN".equalsIgnoreCase(role) && !classroom.getBranchCode().equals(branchCode)) {
            throw new AccessDeniedException("Access denied: classroom is not in your branch");
        }

        // Get all teachers for classroom
        List<AdmissionTeacher> teachers = classroom.getTeachers();

        // Branch-based visibility: any teacher in the branch can see all teachers
        List<AdmissionTeacher> allowedTeachers = teachers.stream()
                .filter(t -> "ADMIN".equalsIgnoreCase(role) || classroom.getBranchCode().equals(branchCode))
                .toList();

        // Map to DTO
        return allowedTeachers.stream()
                .map(t -> new TeacherInfoDTO(t.getId(), t.getTeacherName()))
                .collect(Collectors.toList());
    }


    @Override
    public List<SubjectInfoDTO> getSubjectsByClassRoomId(Long classroomId, String role, String email) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view subjects");
        }

        AdmissionClassRoom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        if (!"ADMIN".equalsIgnoreCase(role) && !classroom.getBranchCode().equals(branchCode)) {
            throw new AccessDeniedException("Access denied: classroom is not in your branch");
        }

        List<AdmissionSubject> subjects = classroom.getSubjects();

        List<AdmissionSubject> allowedSubjects = subjects.stream()
                .filter(s -> "ADMIN".equalsIgnoreCase(role) || classroom.getBranchCode().equals(branchCode))
                .toList();

        return allowedSubjects.stream()
                .map(s -> new SubjectInfoDTO(s.getId(), s.getSubjectName()))
                .collect(Collectors.toList());
    }


}





