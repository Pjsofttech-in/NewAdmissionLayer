package com.newadmission.Controller;

import com.newadmission.DTO.RazorpayVerifyRequest;
import com.newadmission.Service.AdmissionPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public Mono<Map<String, Object>> createOrder(@RequestParam Long admissionId,@RequestParam String role,
                                                 @RequestParam String email, @RequestParam Long amount) {

        return paymentService.createOrder(admissionId, amount,role,email);
    }

    @PostMapping("/verifyAdmissionPayment")
    public Mono<ResponseEntity<String>> verify(@RequestParam String role,
                                               @RequestParam String email,
                                               @RequestBody RazorpayVerifyRequest request)
    {
        request.setSystemName("Admission Management Software");
        return paymentService.verifyPayment(request, role, email)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok("Admission payment success");
                    }
                    return ResponseEntity
                            .badRequest()
                            .body("Payment verification failed");
                });
    }
}
