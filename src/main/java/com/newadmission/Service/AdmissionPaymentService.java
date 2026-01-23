package com.newadmission.Service;

import com.newadmission.DTO.RazorpayVerifyRequest;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface AdmissionPaymentService
{
    Map<String, Object> createOrder(Long admissionId, Long amount, String role, String email);
    Mono<Boolean> verifyPayment(RazorpayVerifyRequest request, String role, String email);
}
