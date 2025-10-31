package com.newadmission.Controller;

import com.newadmission.DTO.StatusResponse;
import com.newadmission.Entity.Installment;
import com.newadmission.Service.InstallmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/installment")
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class InstallmentController {

    @Autowired
    private InstallmentService installmentService;

    @PostMapping("/add/{admissionId}")
    public ResponseEntity<Installment> addInstallment(@PathVariable Long admissionId,
                                                      @RequestBody Installment installment,
                                                      @RequestParam String role,
                                                      @RequestParam String email) {
        return ResponseEntity.ok(installmentService.addInstallmentToAdmission(admissionId, installment, role, email));
    }

    @GetMapping("/getInstallmentsByAdmission")
    public ResponseEntity<List<Installment>> getInstallmentsByAdmission(@RequestParam Long admissionId,
                                                                        @RequestParam String role,
                                                                        @RequestParam String email,
                                                                        @RequestParam String branchCode) {
        List<Installment> installments = installmentService.getInstallmentsByAdmission(admissionId, role, email, branchCode);
        return ResponseEntity.ok(installments);
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id,
                                         @RequestParam String role,
                                         @RequestParam String email) {
        installmentService.deleteInstallment(id, role, email);
        return ResponseEntity.ok("Deleted successfully");
    }

    @GetMapping("/getInstallmentById")
    public ResponseEntity<Installment> getInstallmentById(@RequestParam Long id,
                                                          @RequestParam String role,
                                                          @RequestParam String email,
                                                          @RequestParam String branchCode) {
        Installment installment = installmentService.getInstallmentById(id, role, email, branchCode);
        return ResponseEntity.ok(installment);
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<Installment> update(@PathVariable Long id,
                                              @RequestBody Installment installment,
                                              @RequestParam String role,
                                              @RequestParam String email) {
        return ResponseEntity.ok(installmentService.updateInstallment(id, installment, role, email));
    }

    @PatchMapping("/status/{id}")
    public ResponseEntity<StatusResponse> patchInstallmentStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String role,
            @RequestParam String email) {

        Installment updated = installmentService.updateInstallmentStatus(id, status, role, email);
        return ResponseEntity.ok(new StatusResponse(updated.getStatus()));
    }


}
