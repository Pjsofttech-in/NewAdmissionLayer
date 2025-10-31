package com.newadmission.Controller;

import com.newadmission.Entity.ClassRoomSubjectDetails;
import com.newadmission.Service.ClassRoomSubjectDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class ClassRoomSubjectDetailsController {

    @Autowired
    private ClassRoomSubjectDetailsService service;

    @PostMapping("/createClassRoomSubjectDetails")
    public ResponseEntity<?> create(@RequestBody ClassRoomSubjectDetails details,
                                    @RequestParam String role,
                                    @RequestParam String email) {
        return ResponseEntity.ok(service.create(details, role, email));
    }

    @GetMapping("/getAllClassRoomSubjectDetails")
    public ResponseEntity<?> getAll(@RequestParam String role,
                                    @RequestParam String email,
                                    @RequestParam String branchCode) {
        return ResponseEntity.ok(service.getAll(role, email, branchCode));
    }

    @GetMapping("/getClassRoomSubjectDetailsById/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id,
                                     @RequestParam String role,
                                     @RequestParam String email) {
        return ResponseEntity.ok(service.getById(id, role, email));
    }

    @PutMapping("/updateClassRoomSubjectDetails/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody ClassRoomSubjectDetails details,
                                    @RequestParam String role,
                                    @RequestParam String email) {
        return ResponseEntity.ok(service.update(id, details, role, email));
    }

    @DeleteMapping("/deleteClassRoomSubjectDetails/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id,
                                         @RequestParam String role,
                                         @RequestParam String email) {
        service.delete(id, role, email);
        return ResponseEntity.ok("Deleted successfully");
    }
    @GetMapping("/getIdByTopicName")
    public ResponseEntity<Long> getIdByTopicName(@RequestParam String topicName,
                                                 @RequestParam String role,
                                                 @RequestParam String email) {
        Long id = service.getIdByTopicName(topicName, role, email);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/getTopicNames")
    public ResponseEntity<List<String>> getTopicNamesByFilters(
            @RequestParam Long classroomId,
            @RequestParam Long subjectId,
            @RequestParam Long examTypeId,
            @RequestParam Long paperTypeId,
            @RequestParam String role,
            @RequestParam String email) {

        List<String> topicNames = service.getTopicNamesByFilters(
                classroomId, subjectId, examTypeId, paperTypeId, role, email);
        return ResponseEntity.ok(topicNames);
    }
}
