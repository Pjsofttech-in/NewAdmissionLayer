package com.newadmission.Controller;

import com.newadmission.DTO.RazorpayVerifyRequest;
import com.newadmission.Service.AdmissionPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionPaymentController
{

    @Autowired
    AdmissionPaymentService paymentService;

    @PostMapping("/createAdmissionOrder")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam Long admissionId,
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam Long amount) {

        Map<String, Object> response =
                paymentService.createOrder(admissionId, amount, role, email);

        return ResponseEntity.ok(response);
    }



    @PostMapping("/verifyAdmissionPayment")
    public ResponseEntity<String> verify(
            @RequestParam String role,
            @RequestParam String email,
            @RequestBody RazorpayVerifyRequest request) {

        request.setSystemName("Admission Management Software");

        boolean success = paymentService.verifyPayment(request, role, email);

        if (success) {
            return ResponseEntity.ok("Admission payment success");
        }
        return ResponseEntity.badRequest().body("Payment verification failed");
    }

}
