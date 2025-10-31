package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionPeriod;
import com.newadmission.Service.AdmissionPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionPeriodController {

    @Autowired
    private AdmissionPeriodService periodService;

    @PostMapping("/createPeriod")
    public ResponseEntity<AdmissionPeriod> createPeriod(@RequestBody AdmissionPeriod period,
                                                        @RequestParam String role,
                                                        @RequestParam String email) {
        return ResponseEntity.ok(periodService.createPeriod(period, role, email));
    }

    @GetMapping("/getAllPeriods")
    public ResponseEntity<List<AdmissionPeriod>> getAllPeriod(@RequestParam String role,
                                                        @RequestParam String email,
                                                        @RequestParam String branchCode) {
        return ResponseEntity.ok(periodService.getAllPeriods(role, email, branchCode));
    }

    @GetMapping("/getPeriodById/{id}")
    public ResponseEntity<AdmissionPeriod> getPeriodById(@PathVariable Integer id,
                                                   @RequestParam String role,
                                                   @RequestParam String email) {
        return ResponseEntity.ok(periodService.getPeriodById(id, role, email));
    }

    @PutMapping("/updatePeriod/{id}")
    public ResponseEntity<AdmissionPeriod> updatePeriod(@PathVariable Integer id,
                                                        @RequestBody AdmissionPeriod period,
                                                        @RequestParam String role,
                                                        @RequestParam String email) {
        return ResponseEntity.ok(periodService.updatePeriod(id, period, role, email));
    }

    @DeleteMapping("/deletePeriod/{id}")
    public ResponseEntity<String> deletePeriod(@PathVariable Integer id,
                                         @RequestParam String role,
                                         @RequestParam String email) {
        periodService.deletePeriod(id, role, email);
        return ResponseEntity.ok("Period deleted successfully");
    }
}