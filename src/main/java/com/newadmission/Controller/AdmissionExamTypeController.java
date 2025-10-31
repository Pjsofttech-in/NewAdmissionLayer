package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionExamType;
import com.newadmission.Service.AdmissionExamTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionExamTypeController {

    @Autowired
    private AdmissionExamTypeService service;

    @PostMapping("/createExamType")
    public ResponseEntity<AdmissionExamType> createExamType(@RequestBody AdmissionExamType examType,
                                                            @RequestParam String role,
                                                            @RequestParam String email) {
        return ResponseEntity.ok(service.createExamType(examType, role, email));
    }

    @PutMapping("/updateExamType/{id}")
    public ResponseEntity<AdmissionExamType> updateExamType(@PathVariable Long id,
                                                            @RequestBody AdmissionExamType examType,
                                                            @RequestParam String role,
                                                            @RequestParam String email) {
        return ResponseEntity.ok(service.updateExamType(id, examType, role, email));
    }


    @DeleteMapping("/deleteExamType/{id}")
    public ResponseEntity<String> deleteExamType(@PathVariable Long id,
                                                 @RequestParam String role,
                                                 @RequestParam String email) {
        service.deleteExamType(id, role, email);
        return ResponseEntity.ok("ExamType deleted successfully");
    }

    @GetMapping("/ExamTypegetbyid/{id}")
    public ResponseEntity<AdmissionExamType> getExamTypeById(@PathVariable Long id,
                                                             @RequestParam String role,
                                                             @RequestParam String email) {
        return ResponseEntity.ok(service.getExamTypeById(id, role, email));
    }
    @GetMapping("/getAllExamType")
    public ResponseEntity<List<AdmissionExamType>> getAllExamTypes(@RequestParam String role,
                                                                   @RequestParam String email,
                                                                   @RequestParam String branchCode) {
        return ResponseEntity.ok(service.getAllExamTypes(role, email, branchCode));
    }

    @GetMapping("/examtypebyteacher")
    public ResponseEntity<List<AdmissionExamType>> getExamTypesByTeacherEmailAndBranch(
            @RequestParam String email,
            @RequestParam String branchCode) {

        List<AdmissionExamType> examTypes = service.getExamTypesByTeacherEmailAndBranchCode(email, branchCode);
        return ResponseEntity.ok(examTypes);
    }
}