package com.newadmission.Serviceimpl;

import com.newadmission.DTO.BulkWhatsAppRequest;
import com.newadmission.DTO.WhatsAppRecipientDTO;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.Installment;
import com.newadmission.Repository.AdmissionRepository;
import com.newadmission.Repository.InstallmentRepository;
import com.newadmission.Service.GupshupService;
import com.newadmission.Service.InstallmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class InstallmentServiceImpl implements InstallmentService {

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private AdmissionRepository admissionRepository;

    @Autowired
    private WebClient webClient;

    @Autowired
    private StaffService staffService;

    @Autowired
    private GupshupService gupshupService;


    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    private boolean hasPermission(String role, String email, String action) {
        if ("BRANCH".equalsIgnoreCase(role)) {
            try {
                Boolean exists = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/existBranchbyemail")
                                .queryParam("email", email)
                                .build())
                        .retrieve()
                        .bodyToMono(Boolean.class)
                        .block();

                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return switch (role.toUpperCase()) {
            case "STAFF" -> {
                Map<String, Boolean> perms = staffService.getPermissionsByEmail(email);
                yield switch (action.toUpperCase()) {
                    case "GET" -> Boolean.TRUE.equals(perms.get("cansGet"));
                    case "POST" -> Boolean.TRUE.equals(perms.get("cansPost"));
                    case "PUT" -> Boolean.TRUE.equals(perms.get("cansPut"));
                    case "DELETE" -> Boolean.TRUE.equals(perms.get("cansDelete"));
                    default -> false;
                };
            }
            case "DEPARTMENT" -> {
                Map<String, Object> perms = staffService.getCrudPermissionForDepartmentByEmail(email);
                yield switch (action.toUpperCase()) {
                    case "GET" -> Boolean.TRUE.equals(perms.get("candGet"));
                    case "POST" -> Boolean.TRUE.equals(perms.get("candPost"));
                    case "PUT" -> Boolean.TRUE.equals(perms.get("candPut"));
                    case "DELETE" -> Boolean.TRUE.equals(perms.get("candDelete"));
                    default -> false;
                };
            }
            default -> false;
        };
    }

    private String generateUniqueInvoiceNo() {
        String lastInvoice = installmentRepository.findLatestInvoiceNo();
        int nextNumber = 1;

        if (lastInvoice != null && lastInvoice.startsWith("INV")) {
            try {
                nextNumber = Integer.parseInt(lastInvoice.substring(3)) + 1;
            } catch (NumberFormatException e) {
                // fallback to 1 if parsing fails
                nextNumber = 1;
            }
        }

        return String.format("INV%06d", nextNumber); // e.g., INV000001, INV000002...
    }



    // Method to fetch the branch code based on role
    private String fetchBranchCodeByRole(String role, String email) {
        String endpoint = switch (role.toLowerCase()) {
            case "branch" -> "/branch/getbranchcode";
            case "department" -> "/department/getbranchcode";
            case "staff" -> "/staff/getbranchcode";
            default -> throw new IllegalArgumentException("Invalid role: " + role);
        };

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpoint)
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }


    // Example method where you add an installment
    @Override
    public Installment addInstallmentToAdmission(Long admissionId, Installment installment, String role, String email) {
        // Check permissions (code omitted for brevity)

        // Fetch the admission
        AdmissionForm admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Admission not found with ID: " + admissionId));

        // Get current values
        double totalInstallmentsPaid = installmentRepository.findByAdmissionId(admissionId).stream()
                .mapToDouble(Installment::getAmount)
                .sum();
        double currentPendingFees = admission.getPendingFees();

        // Default status to Pending if not set
        if (installment.getStatus() == null || installment.getStatus().isEmpty()) {
            installment.setStatus("Pending");
        }

        // Only apply amount if status is "Paid"
        if ("Paid".equalsIgnoreCase(installment.getStatus())) {
            // Check if there are pending fees
            if (currentPendingFees <= 0) {
                throw new RuntimeException("No pending fees left for installment.");
            }

            // Check if amount is valid
            if (installment.getAmount() > currentPendingFees) {
                throw new RuntimeException("Installment amount cannot exceed the pending fees.");
            }

            // Update admission fees
            double updatedPendingFees = currentPendingFees - installment.getAmount();
            double updatedPaidFees = totalInstallmentsPaid + installment.getAmount();

            admission.setPendingFees(updatedPendingFees);
            admission.setPaidFees(updatedPaidFees);
            admission.setStatus(updatedPendingFees == 0 ? "Completed" : "In Progress");
        }

        // Set invoice number
        installment.setInvoiceNo(generateUniqueInvoiceNo());

        // Set meta fields
        installment.setCreatedByEmail(email);
        installment.setRole(role);
        String branchCode = fetchBranchCodeByRole(role, email);
        installment.setBranchCode(branchCode);

        // Link and save
        installment.setAdmission(admission);
        admissionRepository.save(admission);
        Installment saved = installmentRepository.save(installment); // Save and capture reference

        // ✅ Normalize mobile number
        String mobileNumber = normalizeToIndianFormat(admission.getMobile1());

        // ✅ WhatsApp Message Sending
        if (mobileNumber != null && admission.getPendingFees() > 0) {
            WhatsAppRecipientDTO recipient = new WhatsAppRecipientDTO();
            recipient.setPhone(mobileNumber); // already normalized to 91XXXXXXXXXX

            recipient.setTemplateId("7e1de0d0-5d7f-4c23-b9ec-dd7029ad6a07");

            List<String> parameters = List.of(
                    "Your Institute Name",                           // {{1}}
                    admission.getName(),                             // {{2}}
                    admission.getCoursename(),                       // {{3}}
                    String.valueOf(admission.getRollNo()),           // {{4}}
                    admission.getAcademicYear(),                     // {{5}}
                    String.valueOf(admission.getTotalFees()),        // {{6}}
                    String.valueOf(admission.getPaidFees()),         // {{7}}
                    String.valueOf(admission.getPendingFees()),      // {{8}}
                    saved.getDueDate() != null
                            ? saved.getDueDate().toString()          // {{9}}
                            : "N/A"
            );

            recipient.setParameters(parameters);

            BulkWhatsAppRequest bulkRequest = new BulkWhatsAppRequest();
            bulkRequest.setRecipients(List.of(recipient));

            gupshupService.sendWhatsAppTemplate(bulkRequest);
        } else {
            System.out.println("Invalid mobile number, WhatsApp not sent: " + admission.getMobile1());
        }

        // ✅ SMS Text Message Sending using same WhatsApp template parameters
        if (mobileNumber != null && admission.getPendingFees() > 0) {
            List<String> smsParams = List.of(
                    "Your Institute Name",                           // {{1}}
                    admission.getName(),                             // {{2}}
                    admission.getCoursename(),                       // {{3}}
                    String.valueOf(admission.getRollNo()),           // {{4}}
                    admission.getAcademicYear(),                     // {{5}}
                    String.valueOf(admission.getTotalFees()),        // {{6}}
                    String.valueOf(admission.getPaidFees()),         // {{7}}
                    String.valueOf(admission.getPendingFees()),      // {{8}}
                    saved.getDueDate() != null
                            ? saved.getDueDate().toString()          // {{9}}
                            : "N/A"
            );

            String smsMessage = String.format(
                    "%s, %s, %s, Roll No: %s, Batch: %s, Total Fees: %s, Paid: %s, Pending: %s, Next Installment: %s",
                    smsParams.get(0), // Institute
                    smsParams.get(1), // Student Name
                    smsParams.get(2), // Course
                    smsParams.get(3), // Roll No
                    smsParams.get(4), // Batch
                    smsParams.get(5), // Total Fees
                    smsParams.get(6), // Paid
                    smsParams.get(7), // Pending
                    smsParams.get(8)  // Next Installment Date
            );

            gupshupService.sendSmsText(mobileNumber, smsMessage);
        } else {
            System.out.println("Invalid mobile number, SMS not sent: " + admission.getMobile1());
        }

        return saved;
    }
    // ✅ Utility to handle +91 / 91 / 10-digit numbers
    private String normalizeToIndianFormat(String rawMobile) {
        if (rawMobile == null || rawMobile.isBlank()) return null;
        rawMobile = rawMobile.replaceAll("[^0-9]", ""); // remove +, spaces, dashes
        if (rawMobile.startsWith("91") && rawMobile.length() == 12) return rawMobile;
        if (rawMobile.length() == 10) return "91" + rawMobile;
        return null; // invalid
    }



    @Override
    public List<Installment> getInstallmentsByAdmission(Long admissionId, String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view installments");
        }
        return installmentRepository.findByAdmissionIdAndBranchCode(admissionId, branchCode);
    }


    @Override
    public void deleteInstallment(Long installmentId, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete installment");
        }

        Installment inst = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        AdmissionForm admission = inst.getAdmission();
        double totalPaid = admission.getPaidFees() - inst.getAmount();
        double pending = admission.getTotalFees() - totalPaid;

        admission.setPaidFees(totalPaid);
        admission.setPendingFees(pending);
        admission.setStatus(pending == 0 ? "Completed" : "In Progress");

        admissionRepository.save(admission);
        installmentRepository.deleteById(installmentId);
    }

    @Override
    public Installment getInstallmentById(Long id, String role, String email, String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view installment");
        }

        // Fetch installment by id and branchCode
        return installmentRepository.findByIdAndBranchCode(id, branchCode)
                .orElseThrow(() -> new RuntimeException("Installment not found"));
    }


    @Override
    public Installment updateInstallment(Long id, Installment updated, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update installment");
        }

        Installment existing = installmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        AdmissionForm admission = existing.getAdmission();
        boolean wasPaid = "Paid".equalsIgnoreCase(existing.getStatus());
        boolean isNowPaid = "Paid".equalsIgnoreCase(updated.getStatus());

        // Case 1: Not Paid → Paid
        if (!wasPaid && isNowPaid) {
            double newPaidFees = admission.getPaidFees() + existing.getAmount();
            double newPendingFees = admission.getTotalFees() - newPaidFees;

            admission.setPaidFees(newPaidFees);
            admission.setPendingFees(newPendingFees);
            admission.setStatus(newPendingFees == 0 ? "Completed" : "In Progress");

            admissionRepository.save(admission);
        }

        // Case 2: Paid → Not Paid
        if (wasPaid && !isNowPaid) {
            double updatedPaidFees = admission.getPaidFees() - existing.getAmount();
            double updatedPendingFees = admission.getTotalFees() - updatedPaidFees;

            admission.setPaidFees(updatedPaidFees);
            admission.setPendingFees(updatedPendingFees);
            admission.setStatus(updatedPendingFees == 0 ? "Completed" : "In Progress");

            admissionRepository.save(admission);
        }

        // ✅ Case 3: Paid → Paid and amount changed
        if (wasPaid && isNowPaid && updated.getAmount() != existing.getAmount()) {
            double oldAmount = existing.getAmount();
            double newAmount = updated.getAmount();

            double difference = oldAmount - newAmount;  // subtracting new from old
            double newPendingFees = admission.getPendingFees() + difference;
            double newPaidFees = admission.getTotalFees() - newPendingFees;

            admission.setPendingFees(newPendingFees);
            admission.setPaidFees(newPaidFees);
            admission.setStatus(newPendingFees == 0 ? "Completed" : "In Progress");

            admissionRepository.save(admission);
        }


        // Update fields
        if (updated.getAmount() != 0) {
            existing.setAmount(updated.getAmount());
        }
        if (updated.getDueDate() != null) {
            existing.setDueDate(updated.getDueDate());
        }
        if (updated.getInstallmentDate() != null) {
            existing.setInstallmentDate(updated.getInstallmentDate());
        }
        if (updated.getStatus() != null && !updated.getStatus().isEmpty()) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getTransactionId() != null && !updated.getTransactionId().isEmpty()) {
            existing.setTransactionId(updated.getTransactionId());
        }
        if (updated.getPaidBy() != null && !updated.getPaidBy().isEmpty()) {
            existing.setPaidBy(updated.getPaidBy());
        }
        if (updated.getRemark() != null && !updated.getRemark().isEmpty()) {
            existing.setRemark(updated.getRemark());
        }
        if (updated.getInstallmentCount() != null && !updated.getInstallmentCount().isEmpty()) {
            existing.setInstallmentCount(updated.getInstallmentCount());
        }
        if (updated.getMonth() != null && !updated.getMonth().isEmpty()) {
            existing.setMonth(updated.getMonth());
        }

        return installmentRepository.save(existing);
    }


    @Override
    public Installment updateInstallmentStatus(Long installmentId, String newStatus, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("No permission to update installment status");
        }

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new RuntimeException("Installment not found"));

        AdmissionForm admission = installment.getAdmission();

        boolean wasPaidBefore = "Paid".equalsIgnoreCase(installment.getStatus());
        boolean isPaidNow = "Paid".equalsIgnoreCase(newStatus);

        // Handle null values for paidFees before performing arithmetic
        double paidFees = (admission.getPaidFees() != null) ? admission.getPaidFees() : 0.0;
        double totalPaid = paidFees + installment.getAmount();

        // Update admission fees only if status changed to "Paid" and was not previously "Paid"
        if (!wasPaidBefore && isPaidNow) {
            double pending = admission.getTotalFees() - totalPaid;

            admission.setPaidFees(totalPaid);
            admission.setPendingFees(pending);
            admission.setStatus(pending == 0 ? "Completed" : "In Progress");

            admissionRepository.save(admission);
        }

        // Update the installment status
        installment.setStatus(newStatus);
        return installmentRepository.save(installment);
    }



}
