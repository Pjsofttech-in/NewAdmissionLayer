package com.newadmission.Serviceimpl;

import com.newadmission.DTO.RazorpayVerifyRequest;
import com.newadmission.Entity.AdmissionPaymentTransaction;
import com.newadmission.Repository.AdmissionPaymentTransactionRepository;
import com.newadmission.Service.AdmissionPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
@Service
public class AdmissionPaymentServiceImpl implements AdmissionPaymentService
{

    private static final String SYSTEM = "Admission Management Software";
    @Autowired
    StaffService staffService;

    @Autowired
    AdmissionPaymentTransactionRepository transactionRepository;

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long admissionId, Long amount,
                                           String role, String email) {

        if (!staffService.hasPermission(role, email, "Post")) {
            throw new AccessDeniedException("You don't have permission to create payment");
        }

        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        // Call payment system (blocking call)
        Map<String, Object> order =
                staffService.createOrder(branchCode, SYSTEM, amount);

        AdmissionPaymentTransaction tx = new AdmissionPaymentTransaction();
        tx.setAdmissionId(admissionId);
        tx.setRazorpayOrderId((String) order.get("orderId"));
        tx.setAmount(amount);
        tx.setStatus("CREATED");
        tx.setBranchCode(branchCode);
        tx.setSystemName(SYSTEM);
        tx.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(tx);

        return order;
    }



    @Override
    @Transactional
    public boolean verifyPayment(RazorpayVerifyRequest request, String role, String email) {

        if (!staffService.hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You don't have permission to verify payment");
        }

        String branchCode = staffService.fetchBranchCodeByRole(role, email);
        request.setBranchCode(branchCode);
        request.setSystemName(SYSTEM);

        AdmissionPaymentTransaction tx = transactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        boolean isValid = staffService.verifyPayment(request).block(); // OK here

        if (isValid) {
            tx.setStatus("SUCCESS");
            tx.setRazorpayPaymentId(request.getRazorpayPaymentId());
        } else {
            tx.setStatus("FAILED");
            tx.setFailureReason("Signature verification failed");
        }

        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        return isValid;
    }


}
