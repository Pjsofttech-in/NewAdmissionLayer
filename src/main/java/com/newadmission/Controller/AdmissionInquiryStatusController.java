package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionInquiryStatus;
import com.newadmission.Service.AdmissionInquiryStatusService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionInquiryStatusController {

    private final AdmissionInquiryStatusService service;

    public AdmissionInquiryStatusController(AdmissionInquiryStatusService service) {
        this.service = service;
    }

    @PostMapping("/createStatus")
    public AdmissionInquiryStatus create(@RequestBody AdmissionInquiryStatus status,
                                         @RequestParam String role,
                                         @RequestParam String email) {
        return service.createStatus(status, role, email);
    }

    @GetMapping("/getAllStatus")
    public List<AdmissionInquiryStatus> getAll(@RequestParam String role,
                                               @RequestParam String email,
                                               @RequestParam String branchCode) {
        return service.getAllStatuses(role, email, branchCode);
    }

    @GetMapping("/getStatusbyId/{id}")
    public AdmissionInquiryStatus getById(@PathVariable Long id,
                                          @RequestParam String role,
                                          @RequestParam String email) {
        return service.getStatusById(id, role, email);
    }

    @PutMapping("/updateStatus/{id}")
    public AdmissionInquiryStatus update(@PathVariable Long id,
                                         @RequestBody AdmissionInquiryStatus status,
                                         @RequestParam String role,
                                         @RequestParam String email) {
        return service.updateStatus(id, status, role, email);
    }

    @DeleteMapping("/deleteStatus/{id}")
    public void delete(@PathVariable Long id,
                       @RequestParam String role,
                       @RequestParam String email) {
        service.deleteStatus(id, role, email);
    }
}