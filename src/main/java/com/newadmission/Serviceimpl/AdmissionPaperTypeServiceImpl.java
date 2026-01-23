package com.newadmission.Serviceimpl;

import com.newadmission.Entity.AdmissionExamType;
import com.newadmission.Entity.AdmissionPaperType;
import com.newadmission.Entity.AdmissionTeacher;
import com.newadmission.Repository.AdmissionPaperTypeRepository;
import com.newadmission.Repository.AdmissionTeacherRepository;
import com.newadmission.Service.AdmissionPaperTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AdmissionPaperTypeServiceImpl implements AdmissionPaperTypeService {

    private final AdmissionPaperTypeRepository repository;
    private final WebClient webClient;
    private final StaffService staffService;
    private final AdmissionTeacherRepository admissionTeacherRepository;
    

    @Autowired
    public AdmissionPaperTypeServiceImpl(AdmissionPaperTypeRepository repository,
                                         WebClient webClient,
                                         StaffService staffService,
                                         AdmissionTeacherRepository admissionTeacherRepository) {
        this.repository = repository;
        this.webClient = webClient;
        this.staffService = staffService;
        this.admissionTeacherRepository = admissionTeacherRepository;
    }
    
    @Override
    public AdmissionPaperType createPaperType(AdmissionPaperType paperType, String role, String email) {
        if (!staffService.hasPermission(role, email, "POST"))
            throw new AccessDeniedException("No permission to create PaperType");

        String branchCode = staffService.fetchBranchCodeByRole(role, email);
        paperType.setRole(role);
        paperType.setCreatedByEmail(email);
        paperType.setBranchCode(branchCode);

        return repository.save(paperType);
    }

    @Override
    public List<AdmissionPaperType> getAll(String role, String email, String branchCode) {
        if (!staffService.hasPermission(role, email, "GET"))
            throw new AccessDeniedException("No permission to view paper types");

        return repository.findAllByBranchCode(branchCode);
    }

    @Override
    public AdmissionPaperType getById(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "GET"))
            throw new AccessDeniedException("No permission to view this paper type");

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PaperType not found"));
    }

    @Override
    public AdmissionPaperType update(Long id, AdmissionPaperType paperType, String role, String email) {
        if (!staffService.hasPermission(role, email, "PUT"))
            throw new AccessDeniedException("No permission to update PaperType");

        AdmissionPaperType existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PaperType not found"));

        existing.setPaperType(paperType.getPaperType());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id, String role, String email) {
        if (!staffService.hasPermission(role, email, "DELETE"))
            throw new AccessDeniedException("No permission to delete PaperType");

        repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PaperType not found"));

        repository.deleteById(id);
    }

    @Override
    public List<AdmissionPaperType> getPaperTypesByTeacherEmailAndBranchCode(String teacherEmail, String branchCode) {
        AdmissionTeacher teacher = admissionTeacherRepository.findByEmailAndBranchCode(teacherEmail, branchCode)
                .orElseThrow(() -> new RuntimeException("Teacher not found with provided email and branch code"));

        return repository.findAllByBranchCode(branchCode);
    }
}