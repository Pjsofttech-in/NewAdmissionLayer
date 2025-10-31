package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionSubject;
import com.newadmission.Service.AdmissionSubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionSubjectController {

    @Autowired
    private AdmissionSubjectService subjectService;

    @PostMapping("/createSubject")
    public ResponseEntity<AdmissionSubject> createSubject(@RequestBody AdmissionSubject subject,
                                                          @RequestParam String role,
                                                          @RequestParam String email) {
        return ResponseEntity.ok(subjectService.createSubject(subject, role, email));
    }

    @GetMapping("/getAllSubjects")
    public ResponseEntity<List<AdmissionSubject>> getAllSubjects(@RequestParam String role,
                                                                 @RequestParam String email,
                                                                 @RequestParam String branchCode) {
        return ResponseEntity.ok(subjectService.getAllSubjects(role, email, branchCode));
    }

    @GetMapping("/getSubjectById/{id}")
    public ResponseEntity<AdmissionSubject> getSubjectById(@PathVariable int id,
                                                           @RequestParam String role,
                                                           @RequestParam String email) {
        return ResponseEntity.ok(subjectService.getSubjectById(id, role, email));
    }

    @PutMapping("/updateSubject/{id}")
    public ResponseEntity<AdmissionSubject> updateSubject(@PathVariable int id,
                                                          @RequestBody AdmissionSubject subject,
                                                          @RequestParam String role,
                                                          @RequestParam String email) {
        return ResponseEntity.ok(subjectService.updateSubject(id, subject, role, email));
    }

    @DeleteMapping("/deleteSubject/{id}")
    public ResponseEntity<String> deleteSubject(@PathVariable int id,
                                                @RequestParam String role,
                                                @RequestParam String email) {
        subjectService.deleteSubject(id, role, email);
        return ResponseEntity.ok("Subject deleted successfully");
    }
}