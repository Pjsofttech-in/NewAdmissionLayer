package com.newadmission.Controller;

import com.newadmission.DTO.StudentResultFilterRequest;
import com.newadmission.DTO.StudentResultResponse;
import com.newadmission.Entity.StudentSubjectResult;
import com.newadmission.Service.StudentSubjectResultService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class StudentSubjectResultController {

    @Autowired
    private StudentSubjectResultService service;

    @PostMapping("/createStudentSubjectResult")
    public ResponseEntity<?> createMultiple(@RequestBody List<StudentSubjectResult> results,
                                            @RequestParam String role,
                                            @RequestParam String email) {
        return ResponseEntity.ok(service.createMultiple(results, role, email));
    }


    @GetMapping("/getStudentSubjectResultById/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id,
                                     @RequestParam String role,
                                     @RequestParam String email) {
        return ResponseEntity.ok(service.getById(id, role, email));
    }

    @PutMapping("/updateStudentSubjectResult/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody StudentSubjectResult result,
                                    @RequestParam String role,
                                    @RequestParam String email) {
        return ResponseEntity.ok(service.update(id, result, role, email));
    }

    @DeleteMapping("/deleteStudentSubjectResult/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id,
                                         @RequestParam String role,
                                         @RequestParam String email) {
        service.delete(id, role, email);
        return ResponseEntity.ok("Deleted successfully");
    }

    @GetMapping("/getResultsByStudentId/{studentId}")
    public ResponseEntity<?> getByStudentId(@PathVariable Long studentId,
                                            @RequestParam String role,
                                            @RequestParam String email) {
        return ResponseEntity.ok(service.getByStudentId(studentId, role, email));
    }


    @GetMapping("/getStudentResult/{studentId}")
     public ResponseEntity<StudentResultResponse> getStudentResultsByStudentId(
        @PathVariable Long studentId,
        @RequestParam String role,
        @RequestParam String email) {

    StudentResultResponse response = service.getStudentResultsByStudentId(studentId, role, email);
    return ResponseEntity.ok(response);
}

    @GetMapping("/getAllStudentResults")
    public ResponseEntity<Page<StudentResultResponse>> getAllStudentResults(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam String branchCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestBody(required = false) StudentResultFilterRequest filterRequest)
    {
        Page<StudentResultResponse> results = service.getAllStudentResults(role, email, branchCode, filterRequest, page, size);
        return ResponseEntity.ok(results);
    }


    @GetMapping("/passfailcount")
    public ResponseEntity<List<Map<String, Object>>> getPassFailCount(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String paperType
    ) {
        return ResponseEntity.ok(service.getPassFailCounts(role, email, examType, paperType));
    }

    @GetMapping("/getuserResult/{studentId}")
    public ResponseEntity<StudentResultResponse> getStudentResultsByStudentId(@PathVariable Long studentId) {
        StudentResultResponse response = service.getStudentResultsByStudentId(studentId);
        return ResponseEntity.ok(response);
    }

}
