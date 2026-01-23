package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionExamType;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.Repository.AdmissionExamTypeRepository;
import com.newadmission.Repository.AdmissionTeacherRepository;
import com.newadmission.Service.AdmissionExamTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AdmissionExamTypeServiceImpl implements AdmissionExamTypeService {

    private final AdmissionExamTypeRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;
    private final AdmissionTeacherRepository admissionTeacherRepository;
    
    @Autowired
    public AdmissionExamTypeServiceImpl(AdmissionExamTypeRepository repository,
                                        WebClient webClient,
                                        StaffService staffService,
                                        AdmissionTeacherRepository admissionTeacherRepository) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
        this.admissionTeacherRepository =admissionTeacherRepository;
    }

    @Override
    public AdmissionExamType createExamType(AdmissionExamType examType, String role, String email) {
        if (!staffService.hasPermission(role, email, "POST"))
            throw new AccessDeniedException("No permission to create ExamType");

        String branchCode = staffService.fetchBranchCodeByRole(role, email);
        examType.setCreatedByEmail(email);
        examType.setRole(role);
        examType.setBranchCode(branchCode);

        return repository.save(examType);
    }

    @Override
    public List<AdmissionExamType> getAllExamTypes(String role, String email, String branchCode) {
        if (!staffService.hasPermission(role, email, "GET"))
            throw new AccessDeniedException("No permission to view ExamType list");

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionExamType getExamTypeById(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "GET"))
            throw new AccessDeniedException("No permission to view ExamType");

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ExamType not found"));
    }

    @Override
    public AdmissionExamType updateExamType(Long id, AdmissionExamType examType, String role, String email) {
        if (!staffService.hasPermission(role, email, "PUT"))
            throw new AccessDeniedException("No permission to update ExamType");

        AdmissionExamType existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ExamType not found"));

        existing.setExamType(examType.getExamType());
        return repository.save(existing);
    }

    @Override
    public void deleteExamType(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "DELETE"))
            throw new AccessDeniedException("No permission to delete ExamType");

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ExamType not found"));

        repository.deleteById(id);
    }

    @Override
    public List<AdmissionExamType> getExamTypesByTeacherEmailAndBranchCode(String teacherEmail, String branchCode) {
        AdmissionTeacher teacher = admissionTeacherRepository.findByEmailAndBranchCode(teacherEmail, branchCode)
                .orElseThrow(() -> new RuntimeException("Teacher not found with provided email and branch code"));

        return repository.findAllByBranchCode(branchCode);
    }

}