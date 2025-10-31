package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionExamType;
import com.newadmission.Entity.AdmissionPaperType;
import com.newadmission.Service.AdmissionPaperTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionPaperTypeController {

    @Autowired
    private AdmissionPaperTypeService service;

    @PostMapping("/createPaperType")
    public ResponseEntity<AdmissionPaperType> createPaperType(@RequestBody AdmissionPaperType paperType,
                                                              @RequestParam String role,
                                                              @RequestParam String email) {
        return ResponseEntity.ok(service.createPaperType(paperType, role, email));
    }

    @PutMapping("/updatePaperType/{id}")
    public ResponseEntity<AdmissionPaperType> updatePaperType(@PathVariable Long id,
                                                              @RequestBody AdmissionPaperType paperType,
                                                              @RequestParam String role,
                                                              @RequestParam String email) {
        return ResponseEntity.ok(service.update(id, paperType, role, email));
    }

    @DeleteMapping("/deletePaperType/{id}")
    public ResponseEntity<String> deletePaperType(@PathVariable Long id,
                                                  @RequestParam String role,
                                                  @RequestParam String email) {
        service.delete(id, role, email);
        return ResponseEntity.ok("PaperType deleted successfully");
    }

    @GetMapping("/PaperTypegetById/{id}")
    public ResponseEntity<AdmissionPaperType> getPaperTypeById(@PathVariable Long id,
                                                               @RequestParam String role,
                                                               @RequestParam String email) {
        return ResponseEntity.ok(service.getById(id, role, email));
    }

    @GetMapping("/getAllPaperType")
    public ResponseEntity<List<AdmissionPaperType>> getAllPaperTypes(@RequestParam String role,
                                                                     @RequestParam String email,
                                                                     @RequestParam String branchCode) {
        return ResponseEntity.ok(service.getAll(role, email, branchCode));
    }

    @GetMapping("/papertypebyteacher")
    public ResponseEntity<List<AdmissionPaperType>> getpaperTypesByTeacherEmailAndBranch(
            @RequestParam String email,
            @RequestParam String branchCode) {

        List<AdmissionPaperType> examTypes = service.getPaperTypesByTeacherEmailAndBranchCode(email, branchCode);
        return ResponseEntity.ok(examTypes);
    }
}