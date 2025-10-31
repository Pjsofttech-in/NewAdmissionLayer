package com.newadmission.Controller;

import com.newadmission.DTO.PeriodAssignmentRequest;
import com.newadmission.Entity.AdmissionTimetable;
import com.newadmission.Service.AdmissionTimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionTimetableController {

    @Autowired
    private AdmissionTimetableService timetableService;



    @PostMapping("/createTimetable")
    public ResponseEntity<AdmissionTimetable> createTimetable(
            @RequestParam String weekday,
            @RequestParam Long classRoomId,
            @RequestBody List<PeriodAssignmentRequest> assignments,
            @RequestParam String role,
            @RequestParam String email
    ){
        return ResponseEntity.ok(
                timetableService.createTimetableWithAssignments(weekday,classRoomId,assignments,role,email)
        );
    }

    @PutMapping("/updateTimetableAssignments/{id}")
    public ResponseEntity<AdmissionTimetable> updateTimetable(
            @PathVariable int id,
            @RequestBody List<PeriodAssignmentRequest> assignments,
            @RequestParam String role,
            @RequestParam String email
    ){
        return ResponseEntity.ok(
                timetableService.updateTimetableAssignments(id,assignments,role,email)
        );
    }

    @GetMapping("/getAllTimetables")
    public ResponseEntity<List<AdmissionTimetable>> getAll(@RequestParam String role,
                                                           @RequestParam String email,
                                                           @RequestParam String branchCode){
        return ResponseEntity.ok(
                timetableService.getAllTimetables(role,email,branchCode)
        );
    }

    @GetMapping("/getTimetableById/{id}")
    public ResponseEntity<AdmissionTimetable> getById(@PathVariable int id,
                                                      @RequestParam String role,
                                                      @RequestParam String email){
        return ResponseEntity.ok(
                timetableService.getTimetableById(id,role,email)
        );
    }

    @DeleteMapping("/deleteTimetable/{id}")
    public ResponseEntity<String> delete(@PathVariable int id,
                                         @RequestParam String role,
                                         @RequestParam String email){
        timetableService.deleteTimetable(id,role,email);
        return ResponseEntity.ok("Deleted successfully");
    }

    @GetMapping("/getTimetableByClassRoomId")
    public ResponseEntity<List<AdmissionTimetable>> getTimetablesByClassroom(
            @RequestParam Integer classRoomId,
            @RequestParam String role,
            @RequestParam String email
    ) {
        List<AdmissionTimetable> timetables = timetableService.getTimetablesByClassroom(classRoomId, role, email);
        return ResponseEntity.ok(timetables);
    }

    @PostMapping("/markPeriodOff")
    public ResponseEntity<String> markPeriodOff(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam Integer timetableId,
            @RequestParam Integer periodId
    ) {
        String result = timetableService.markPeriodOff(role, email, timetableId, periodId);
        return ResponseEntity.ok(result);
    }



}