package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionTermsAndCondition;
import com.newadmission.Service.AdmissionTermsAndConditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionTermsAndConditionController {

    @Autowired
    private AdmissionTermsAndConditionService service;

    @PostMapping("/createTerms")
    public ResponseEntity<AdmissionTermsAndCondition> createTerm(@RequestBody AdmissionTermsAndCondition term,
                                                                 @RequestParam String role,
                                                                 @RequestParam String email) {
        return ResponseEntity.ok(service.createTerm(term, role, email));
    }

    @GetMapping("/getAllTerms")
    public ResponseEntity<List<AdmissionTermsAndCondition>> getAllTerms(@RequestParam String role,
                                                                        @RequestParam String email,
                                                                        @RequestParam String branchCode) {
        return ResponseEntity.ok(service.getAllTerms(role, email, branchCode));
    }

    @GetMapping("/getTermById/{id}")
    public ResponseEntity<AdmissionTermsAndCondition> getTermById(@PathVariable Long id,
                                                                  @RequestParam String role,
                                                                  @RequestParam String email) {
        return ResponseEntity.ok(service.getTermById(id, role, email));
    }

    @PutMapping("/updateTerm/{id}")
    public ResponseEntity<AdmissionTermsAndCondition> updateTerm(@PathVariable Long id,
                                                                 @RequestBody AdmissionTermsAndCondition term,
                                                                 @RequestParam String role,
                                                                  @RequestParam String email) {
        return ResponseEntity.ok(service.updateTerm(id, term, role, email));
    }

    @DeleteMapping("/deleteTerm/{id}")
    public ResponseEntity<String> deleteTerm(@PathVariable Long id,
                                             @RequestParam String role,
                                             @RequestParam String email) {
        service.deleteTerm(id, role, email);
        return ResponseEntity.ok("Term deleted successfully");
    }
}
