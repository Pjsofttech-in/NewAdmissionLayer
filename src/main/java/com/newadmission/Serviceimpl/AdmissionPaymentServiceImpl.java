package com.newadmission.Serviceimpl;

import com.newadmission.DTO.RazorpayVerifyRequest;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.AdmissionPaymentTransaction;
import com.newadmission.Repository.AdmissionPaymentTransactionRepository;
import com.newadmission.Repository.AdmissionRepository;
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
    AdmissionRepository admissionRepository;

    @Autowired
    AdmissionPaymentTransactionRepository transactionRepository;

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long admissionId, Long amount,
                                           String role, String email) {

        if (!staffService.hasPermission(role, email, "Post")) {
            throw new AccessDeniedException("You don't have permission to create payment");
        }

        // 2. Fetch the Admission record
        AdmissionForm admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new IllegalArgumentException("Admission record not found"));

        // 3. Safely get the pending amount (fallback to 0.0 if null)
        double pendingAmount = admission.getPendingFees() != null ? admission.getPendingFees() : 0.0;

        // 4. THE SAFEGUARD: Convert and Check the amounts
        // Note: If the 'amount' parameter is coming in as Paise (e.g., 50000 for ₹500),
        // you must divide by 100 before comparing it to the database value.
        // If the frontend sends it as Rupees (e.g., 500), remove the "/ 100.0".

         double requestedAmountInRupees = amount.doubleValue() / 100.0; // <-- Use this if 'amount' is in paise

        if (requestedAmountInRupees <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        if (requestedAmountInRupees > pendingAmount) {
            throw new IllegalArgumentException("Payment failed: The requested amount ("
                    + requestedAmountInRupees + ") exceeds the pending balance ("
                    + pendingAmount + ").");
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
    public boolean verifyPayment(
            RazorpayVerifyRequest request,
            String role,
            String email
    ) {

        // permission check
        if (!staffService.hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("You don't have permission to verify payment");
        }

        // enrich request
        String branchCode = staffService.fetchBranchCodeByRole(role, email);
        request.setBranchCode(branchCode);
        request.setSystemName("Admission Management Software");

        // fetch transaction
        AdmissionPaymentTransaction tx =
                transactionRepository
                        .findByRazorpayOrderIdAndSystemName(
                                request.getRazorpayOrderId(),
                                request.getSystemName()
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Transaction not found")
                        );

        // 🔥 CALL CLIENT SERVICE
        boolean isValid = staffService.verifyPayment(request).block();

        // update transaction
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
