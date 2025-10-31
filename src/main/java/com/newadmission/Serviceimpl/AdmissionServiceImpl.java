package com.newadmission.Serviceimpl;

import com.beust.jcommander.internal.Nullable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.newadmission.DTO.*;
import com.newadmission.Entity.AdmissionClassRoom;
import com.newadmission.Entity.AdmissionForm;
import com.newadmission.Entity.AdmissionRulesAndRegulations;
import com.newadmission.Entity.Installment;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Pegination.AdmissionSpecification;
import com.newadmission.Repository.AdmissionClassRoomRepository;
import com.newadmission.Repository.AdmissionRepository;
import com.newadmission.Repository.AdmissionRulesAndRegulationsRepository;
import com.newadmission.Repository.AdmissionSubjectRepository;
import com.newadmission.Service.AdmissionService;
import com.newadmission.Service.EmailService;
import com.newadmission.Service.GupshupService;
import com.newadmission.Service.OtpService;
import com.opencsv.CSVReaderHeaderAware;
import io.jsonwebtoken.Claims;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdmissionServiceImpl implements AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final WebClient webClient;
    private final StaffService staffService;
    private final S3Service s3Service;
    private final AdmissionSubjectRepository admissionSubjectRepository;

    @Autowired
    private  AdmissionRulesAndRegulationsRepository admissionRulesAndRegulationsRepository;


    @Autowired
    private AdmissionClassRoomRepository classRoomRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StaffService staffLoginService;

    @Autowired
    private GupshupService gupshupService;



    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OtpService otpService;

    @Value("${client.superadmin.base-url}")
    private String superAdminBaseUrl;

    @Autowired
    public AdmissionServiceImpl(AdmissionRepository admissionRepository, WebClient webClient, StaffService staffService, S3Service s3Service,AdmissionSubjectRepository admissionSubjectRepository) {
        this.admissionRepository = admissionRepository;
        this.webClient = webClient;
        this.staffService = staffService;
        this.s3Service = s3Service;
        this.admissionSubjectRepository = admissionSubjectRepository;
    }

    private boolean hasPermission(String role, String email, String action) {

        if ("SUPERADMIN".equalsIgnoreCase(role) && "GET".equalsIgnoreCase(action)) {
            try {
                Boolean exists = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/checkClientEmailExist")
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
        if ("USER".equalsIgnoreCase(role)) {
            return "POST".equalsIgnoreCase(action);
        }
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

    @Override
    public AdmissionForm createAdmission(AdmissionForm admissionForm, String role, String email, MultipartFile studentImage, MultipartFile paymentImage,MultipartFile admissionPdf,List<Long> rulesIds,String authHeader) {
        // Permission check
        if ("USER".equalsIgnoreCase(role)) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Authorization token is required for USER role");
            }
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.extractAllClaims(token);

                // Decode branchCode
                String encodedBranchCode = claims.get("branchCode", String.class);
                if (encodedBranchCode == null || encodedBranchCode.isEmpty()) {
                    throw new RuntimeException("Invalid token: missing branchCode");
                }
                String decodedBranchCode = new String(Base64.getUrlDecoder().decode(encodedBranchCode), StandardCharsets.UTF_8);
                admissionForm.setBranchCode(decodedBranchCode);

                // Get role and email from token
                String decodedRole = claims.get("role", String.class);
                String decodedEmail = claims.get("email", String.class);

                if (decodedRole == null || decodedRole.isEmpty()) decodedRole = "USER";

                // Override role/email to avoid tampering
                role = decodedRole;
                email = decodedEmail;

                // Set in form
                admissionForm.setRole(decodedRole);
                admissionForm.setCreatedByEmail(decodedEmail);

            } catch (Exception ex) {
                throw new RuntimeException("Failed to decode token for USER role: " + ex.getMessage(), ex);
            }
        }
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to create AdmissionForm");
        }

        // Upload student image if provided
        if (studentImage != null && !studentImage.isEmpty()) {
            try {
                String correctBranchCode;
                if ("USER".equalsIgnoreCase(role)) {
                    correctBranchCode = admissionForm.getBranchCode(); // Already set by controller
                } else {
                    correctBranchCode = fetchBranchCodeByRole(role, email);
                }
                String systemName = "attendance-sys";
                String studentImageUrl = s3Service.uploadFileToDocs(studentImage, correctBranchCode, systemName);
                admissionForm.setStudentImage(studentImageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload student image to S3", e);
            }
        }

        if (admissionForm.getAdmissionPdf() == null && admissionPdf != null && !admissionPdf.isEmpty()) {
            try {
                String correctBranchCode;
                if ("USER".equalsIgnoreCase(role)) {
                    correctBranchCode = admissionForm.getBranchCode();
                } else {
                    correctBranchCode = fetchBranchCodeByRole(role, email);
                }
                String systemName = "attendance-sys";
                String admissionPdfUrl = s3Service.uploadFileToDocs(admissionPdf, correctBranchCode, systemName);
                admissionForm.setAdmissionPdf(admissionPdfUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload admission PDF to S3", e);
            }
        }

        // Encode password if present
        if (admissionForm.getPassword() != null && !admissionForm.getPassword().isEmpty()) {
            admissionForm.setPassword(passwordEncoder.encode(admissionForm.getPassword()));
        }

        // Set role, createdByEmail, and branchCode
        if (!"USER".equalsIgnoreCase(role)) {
            admissionForm.setBranchCode(fetchBranchCodeByRole(role, email));
            admissionForm.setCreatedByEmail(email);
            admissionForm.setRole(role);
        }

        // Auto-fill expiredate
        if (admissionForm.getDate() != null && admissionForm.getDuration() != null) {
            try {
                String durationStr = admissionForm.getDuration().toLowerCase().trim();
                int number = Integer.parseInt(durationStr.replaceAll("[^0-9]", ""));
                if (durationStr.contains("year")) {
                    admissionForm.setExpiredate(admissionForm.getDate().plusYears(number));
                } else if (durationStr.contains("month")) {
                    admissionForm.setExpiredate(admissionForm.getDate().plusMonths(number));
                }
            } catch (Exception e) {
                System.out.println("Unable to parse duration or calculate expiredate: " + e.getMessage());
            }
        }

        // Generate registration number
        if (admissionForm.getRegistrationNo() == null || admissionForm.getRegistrationNo().isEmpty()) {
            int currentYear = LocalDate.now().getYear();
            int nextCounter = 1;
            Optional<AdmissionForm> lastAdmissionOpt = admissionRepository.findTopByOrderByIdDesc();
            if (lastAdmissionOpt.isPresent()) {
                String lastRegNo = lastAdmissionOpt.get().getRegistrationNo();
                if (lastRegNo != null && lastRegNo.length() >= 8) {
                    try {
                        nextCounter = Integer.parseInt(lastRegNo.substring(4)) + 1;
                    } catch (NumberFormatException e) {
                        nextCounter = 1;
                    }
                }
            }
            admissionForm.setRegistrationNo(currentYear + String.format("%04d", nextCounter));
        }

        // ✅ Handle rulesIds → set ManyToMany
        if (rulesIds != null && !rulesIds.isEmpty()) {
            List<AdmissionRulesAndRegulations> rulesList = admissionRulesAndRegulationsRepository.findAllById(rulesIds);
            admissionForm.setRulesAndRegulationsList(rulesList);
        }

        if (paymentImage != null && !paymentImage.isEmpty()) {
            try {
                String correctBranchCode;
                if ("USER".equalsIgnoreCase(role)) {
                    correctBranchCode = admissionForm.getBranchCode();
                } else {
                    correctBranchCode = fetchBranchCodeByRole(role, email);
                }
                String systemName = "attendance-sys";
                String paymentImageUrl = s3Service.uploadFileToDocs(paymentImage, correctBranchCode, systemName);
                admissionForm.setPaymentImage(paymentImageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload payment image to S3", e);
            }
        }

        // Save to DB
        AdmissionForm savedAdmission = admissionRepository.save(admissionForm);

        // Send confirmation email
        if (admissionForm.getEmail() != null && !admissionForm.getEmail().isEmpty()) {
            try {
                emailService.sendAdmissionConfirmation(
                        admissionForm.getEmail(),
                        admissionForm.getName(),
                        admissionForm.getCoursename(),
                        admissionForm.getDate() != null
                                ? admissionForm.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                : "Not Provided"
                );
            } catch (Exception e) {
                System.err.println("⚠ Failed to send admission confirmation email: " + e.getMessage());
            }
        }

        // ✅ Send WhatsApp notification in same style as manualMarkAttendance
        try {
            if (savedAdmission.getMobile1() != null && !savedAdmission.getMobile1().isBlank()) {
                BulkWhatsAppRequest waRequest = new BulkWhatsAppRequest();
                List<WhatsAppRecipientDTO> recipients = new ArrayList<>();
                WhatsAppRecipientDTO recipient = new WhatsAppRecipientDTO();

                recipient.setPhone(formatPhoneNumber(savedAdmission.getMobile1()));
                recipient.setTemplateId("10acb66c-26d7-4b4e-bdac-61e8541a6ab8"); // payment_confirmed template ID

                recipient.setParameters(List.of(
                        savedAdmission.getName() != null ? savedAdmission.getName() : "",  // {{1}} Customer Name
                        savedAdmission.getPaidFees() != null ? String.valueOf(savedAdmission.getPaidFees()) : "0", // {{2}} Amount
                        getInstituteNameFromBranchCode(savedAdmission.getBranchCode()), // {{3}} Institute
                        savedAdmission.getAcademicYear() != null ? savedAdmission.getAcademicYear() : "", // {{4}} Academic Year
                        savedAdmission.getEmail() != null ? savedAdmission.getEmail() : "", // {{5}} Email ID
                        savedAdmission.getCoursename() != null ? savedAdmission.getCoursename() : "", // {{6}} Course Name
                        savedAdmission.getDate() != null
                                ? savedAdmission.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                : "" // {{7}} Date
                ));

                recipients.add(recipient);
                waRequest.setRecipients(recipients);
                gupshupService.sendWhatsAppTemplate(waRequest);
            }
        } catch (Exception ex) {
            System.err.println("⚠ Failed to send WhatsApp message: " + ex.getMessage());
        }

        return savedAdmission;
    }

    // ✅ Same helper from manualMarkAttendance
    private String getInstituteNameFromBranchCode(String branchCode) {
        try {
            String instituteEmail = staffService.getInstituteEmailByBranchCode(branchCode).block();
            if (instituteEmail == null || instituteEmail.isBlank()) {
                return "Unknown Institute";
            }
            List<InstituteLoginResponse> institutes = staffService.getInstituteDetailsOnly(instituteEmail);
            if (institutes != null && !institutes.isEmpty()) {
                return institutes.get(0).getInstituteName();
            }
        } catch (Exception e) {
            System.err.println("Error fetching institute name: " + e.getMessage());
        }
        return "Unknown Institute";
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "";
        phone = phone.trim().replaceAll("[^0-9]", "");
        if (phone.startsWith("91") && phone.length() == 12) {
            return "+" + phone;
        }
        if (phone.length() == 10) {
            return "+91" + phone;
        }
        return "+" + phone;
    }


    @Override
    public Map<String, Object> getAllAdmissions(AdmissionFilterRequest request) {
        // Permission check
        if (!hasPermission(request.getRole(), request.getEmail(), "GET")) {
            throw new AccessDeniedException("No permission to view Admissions");
        }

        // Ensure branchCode is set
        if (request.getBranchCode() == null || request.getBranchCode().isEmpty()) {
            String branchCode = fetchBranchCodeByRole(request.getRole(), request.getEmail());
            request.setBranchCode(branchCode);
        }

        // 🔹 Use Specification for all dynamic filters including createdByEmail
        Specification<AdmissionForm> spec = AdmissionSpecification.withDynamicFilters(request);

        // Pagination & sorting
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("id").descending());
        Page<AdmissionForm> admissionsPage = admissionRepository.findAll(spec, pageable);

        // Full list for summary
        List<AdmissionForm> fullList = admissionRepository.findAll(spec);

        long totalAdmissions = fullList.size();
        double totalFees = fullList.stream().mapToDouble(a -> a.getTotalFees() != null ? a.getTotalFees() : 0.0).sum();
        double paidFees = fullList.stream().mapToDouble(a -> a.getPaidFees() != null ? a.getPaidFees() : 0.0).sum();
        double pendingFees = fullList.stream().mapToDouble(a -> a.getPendingFees() != null ? a.getPendingFees() : 0.0).sum();

        LocalDate today = LocalDate.now();
        String branchCode = request.getBranchCode();

        double currentPending = fullList.stream()
                .filter(a -> branchCode.equals(a.getBranchCode()))
                .flatMap(a -> a.getInstallments().stream())
                .filter(inst -> inst.getStatus() == null || !"PAID".equalsIgnoreCase(inst.getStatus()))
                .filter(inst -> inst.getDueDate() != null && !inst.getDueDate().isAfter(today))
                .mapToDouble(Installment::getAmount)
                .sum();

        double futurePending = fullList.stream()
                .filter(a -> branchCode.equals(a.getBranchCode()))
                .flatMap(a -> a.getInstallments().stream())
                .filter(inst -> inst.getStatus() == null || !"PAID".equalsIgnoreCase(inst.getStatus()))
                .filter(inst -> inst.getDueDate() != null && inst.getDueDate().isAfter(today))
                .mapToDouble(Installment::getAmount)
                .sum();

        // Summary map
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAdmissions", totalAdmissions);
        summary.put("totalFees", totalFees);
        summary.put("paidFees", paidFees);
        summary.put("pendingFees", pendingFees);
        summary.put("currentPending", currentPending);
        summary.put("futurePending", futurePending);

        // Pagination info
        Map<String, Object> page = new HashMap<>();
        page.put("size", admissionsPage.getSize());
        page.put("number", admissionsPage.getNumber());
        page.put("totalElements", totalAdmissions);
        page.put("totalPages", (int) Math.ceil((double) totalAdmissions / request.getSize()));

        // Final response
        Map<String, Object> response = new HashMap<>();
        response.put("summary", summary);
        response.put("admissions", admissionsPage.getContent());
        response.put("page", page);

        return response;
    }








    @Override
//    @CacheEvict(value = "admissionById", key = "#id + '-' + #role + '-' + #email")
    public AdmissionForm updateAdmission(Long id, String admissionJson, MultipartFile studentImage,MultipartFile paymentImage, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new RuntimeException("You do not have permission to update admission");
        }

        AdmissionForm existingAdmissionForm = admissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdmissionForm not found with id: " + id));

        if (admissionJson != null && !admissionJson.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            try {
                AdmissionForm partial = objectMapper.readValue(admissionJson, AdmissionForm.class);

                if (partial.getName() != null) existingAdmissionForm.setName(partial.getName());
                if (partial.getMobile1() != null) existingAdmissionForm.setMobile1(partial.getMobile1());
                if (partial.getDate() != null) existingAdmissionForm.setDate(partial.getDate());
                if (partial.getCoursename() != null) existingAdmissionForm.setCoursename(partial.getCoursename());
                if (partial.getDuration() != null) existingAdmissionForm.setDuration(partial.getDuration());
                if (partial.getEmail() != null) existingAdmissionForm.setEmail(partial.getEmail());
                if (partial.getMobile2() != null) existingAdmissionForm.setMobile2(partial.getMobile2());
                if (partial.getTransactionId() != null) existingAdmissionForm.setTransactionId(partial.getTransactionId());
                if (partial.getTotalFees() != null) existingAdmissionForm.setTotalFees(partial.getTotalFees());
                if (partial.getRemark() != null) existingAdmissionForm.setRemark(partial.getRemark());
                if (partial.getStatus() != null) existingAdmissionForm.setStatus(partial.getStatus());
                if (partial.getDueDate() != null) existingAdmissionForm.setDueDate(partial.getDueDate());
                if (partial.getMediumName() != null) existingAdmissionForm.setMediumName(partial.getMediumName());
                if (partial.getPaymentMethod() != null) existingAdmissionForm.setPaymentMethod(partial.getPaymentMethod());
                if (partial.getPendingFees() != null) existingAdmissionForm.setPendingFees(partial.getPendingFees());
                if (partial.getPaidFees() != null) existingAdmissionForm.setPaidFees(partial.getPaidFees());
//                if (partial.getGuideName() != null) existingAdmissionForm.setGuideName(partial.getGuideName());
                if (partial.getSourceBy() != null) existingAdmissionForm.setSourceBy(partial.getSourceBy());
                if (partial.getPaymentMode() != null) existingAdmissionForm.setPaymentMode(partial.getPaymentMode());
                if (partial.getCurrentAddress() != null) existingAdmissionForm.setCurrentAddress(partial.getCurrentAddress());
                if (partial.getPermanentAddress() != null) existingAdmissionForm.setPermanentAddress(partial.getPermanentAddress());
                if (partial.getAcademicYear() != null) existingAdmissionForm.setAcademicYear(partial.getAcademicYear());
                if (partial.getRollNo() != 0) existingAdmissionForm.setRollNo(partial.getRollNo());
                if (partial.getPaymentDate() != null) existingAdmissionForm.setPaymentDate(partial.getPaymentDate());
                if (partial.getGender() != null) existingAdmissionForm.setGender(partial.getGender());
                if (partial.getDob() != null) existingAdmissionForm.setDob(partial.getDob());
                if(partial.getExpiredate() !=null)existingAdmissionForm.setExpiredate(partial.getExpiredate());
                if(partial.getReference() !=null)existingAdmissionForm.setReference(partial.getReference());
                if (partial.getParentEmail() != null) existingAdmissionForm.setParentEmail(partial.getParentEmail());
                if (partial.getStudentType()!=null)existingAdmissionForm.setStudentType(partial.getStudentType());
                if(partial.getStream()!=null)existingAdmissionForm.setStream(partial.getStream());
                if (partial.getClassType()!=null)existingAdmissionForm.setStream(partial.getStream());
                if (partial.getPaymentImage() != null) existingAdmissionForm.setPaymentImage(partial.getPaymentImage());




            } catch (JsonProcessingException e) {
                throw new RuntimeException("Invalid admission data", e);
            }
        }

        if (studentImage != null && !studentImage.isEmpty()) {
            try {
                String branchCode = existingAdmissionForm.getBranchCode();
                String systemName = "admission-sys";

                // If classroom is assigned
                if (existingAdmissionForm.getAdmissionClassRoom() != null) {
                    Long admissionId = existingAdmissionForm.getId();

                    // Upload to attendance_faces folder
                    String studentImageUrl = s3Service.uploadFileToAttendanceFaces(
                            studentImage,
                            branchCode,
                            systemName,
                            admissionId
                    );
                    existingAdmissionForm.setStudentImage(studentImageUrl);

                    // Copy with rollNo if present
                    if (existingAdmissionForm.getRollNo() != null) {
                        String copiedUrl = s3Service.copyImageToAttendanceFolderWithRollNoFilename(
                                admissionId,
                                existingAdmissionForm.getAdmissionClassRoom().getId(),
                                existingAdmissionForm.getRollNo(),
                                systemName
                        );
                        existingAdmissionForm.setStudentImage(copiedUrl);
                    }
                } else {
                    // No classroom assigned → upload to docs folder
                    String studentImageUrl = s3Service.uploadFileToDocs(
                            studentImage,
                            branchCode,
                            systemName
                    );
                    existingAdmissionForm.setStudentImage(studentImageUrl);
                }

            } catch (IOException e) {
                throw new RuntimeException("Failed to upload student image to S3", e);
            }
        }

        existingAdmissionForm.setRole(role);
        existingAdmissionForm.setCreatedByEmail(email);

        return admissionRepository.save(existingAdmissionForm);


    }


    @Override
//    @CacheEvict(value = "admissionById", key = "#id + '-' + #role + '-' + #email")
    public void deleteAdmission(Long id, String role, String email) {
        if (!hasPermission(role, email, "DELETE")) {
            throw new AccessDeniedException("No permission to delete AdmissionForm");
        }

        admissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdmissionForm not found"));

        admissionRepository.deleteById(id);
    }

    @Override
//    @Cacheable(value = "admissionById", key = "#id + '-' + #role + '-' + #email", unless = "#result == null")
    public AdmissionForm getAdmissionById(Long id, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view AdmissionForm");
        }

        return admissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AdmissionForm not found"));
    }

    @Override
//    @Cacheable(value = "admissionByDate", key = "#startDate + '-' + #endDate + '-' + #role + '-' + #email", unless = "#result == null or #result.isEmpty()")
    public List<AdmissionForm> getAdmissionsByDateFilter(String filterType, LocalDate startDate, LocalDate endDate, String role, String email, String branchCode) {

        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view Admissions");
        }

        List<String> branchCodes;

        if ("superadmin".equalsIgnoreCase(role)) {
            // Get all branches for the superadmin
            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (response == null || response.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> allBranchCodes = new ArrayList<>(response.values());

            if (branchCode == null || branchCode.isBlank()) {
                // ✅ If no branchCode passed → return all branches
                branchCodes = allBranchCodes;
            } else {
                // ✅ If branchCode is passed → check access
                if (!allBranchCodes.contains(branchCode)) {
                    return new ArrayList<>();
                }
                branchCodes = List.of(branchCode);
            }

        } else {
            // For other roles
            if (branchCode == null || branchCode.isBlank()) {
                branchCode = fetchBranchCodeByRole(role, email);
            }
            branchCodes = List.of(branchCode);
        }

        // Date filter logic
        LocalDate today = LocalDate.now();
        LocalDate fromDate;
        LocalDate toDate = today;

        switch (filterType.toLowerCase()) {
            case "today" -> fromDate = today;
            case "last7days" -> fromDate = today.minusDays(7);
            case "last30days" -> fromDate = today.minusDays(30);
            case "last365days" -> fromDate = today.minusDays(365);
            case "custom" -> {
                if (startDate == null || endDate == null) {
                    throw new IllegalArgumentException("Start and end date required for custom filter");
                }
                fromDate = startDate;
                toDate = endDate;
            }
            default -> throw new IllegalArgumentException("Invalid filter type: " + filterType);
        }

        return admissionRepository.findByDateBetweenAndBranchCode(fromDate, toDate, branchCodes);
    }




@Override
//@Cacheable(
//        value = "studentsByClassFilter",
//        key = "#academicYear + '-' + #courseName + '-' + #mediumName + '-' + #role + '-' + #email",
//        unless = "#result == null or #result.isEmpty()"
//)
public List<AdmissionForm> filterStudentsByClassroom(String academicYear, String mediumName, String courseName, String role, String email) {
    // Step 0: Permission check
    if (!hasPermission(role, email, "GET")) {
        throw new AccessDeniedException("You do not have permission to filter students by classroom");
    }

    // Step 1: Fetch branchCode from email and role
    String branchCode = fetchBranchCodeByRole(role, email);

    // ✅ Step 2: Directly call the repository method to get students without classroom
    return admissionRepository.findByAcademicYearAndCoursenameAndMediumNameAndBranchCode(
            academicYear, courseName, mediumName, branchCode
    );
}



    @Override
//    @Cacheable(value = "admissionsByClassroomId", key = "#classroomId + '-' + #role + '-' + #email", unless = "#result == null or #result.isEmpty()")
    public List<AdmissionForm> getAdmissionsByClassroomId(Long classroomId, String role, String email) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to fetch Admissions");
        }

        String branchCode = fetchBranchCodeByRole(role, email);

        AdmissionClassRoom classRoom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Classroom not found with ID: " + classroomId));

        if (!branchCode.equals(classRoom.getBranchCode())) {
            throw new AccessDeniedException("Access to this classroom is denied");
        }

        return admissionRepository.findByAdmissionClassRoomIdAndBranchCode(classroomId, branchCode);
    }


    @Override
//    @Cacheable(
//            value = "admissionStatsCache",
//            key = "'admissionStats_' + #role + '_' + #email + '_' + (#branchCode != null ? #branchCode : 'all')"
//    )
    public Map<String, Object> getAdmissionStats(String role, String email, @Nullable String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view stats");
        }

        List<String> branchCodes;

        if ("superadmin".equalsIgnoreCase(role)) {
            // Fetch all branches under superadmin
            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (response == null || response.isEmpty()) {
                return new HashMap<>();
            }

            List<String> allBranchCodes = new ArrayList<>(response.values());

            if (branchCode == null || branchCode.isBlank()) {
                branchCodes = allBranchCodes;
            } else {
                if (!allBranchCodes.contains(branchCode)) {
                    return new HashMap<>();
                }
                branchCodes = List.of(branchCode);
            }

        } else {
            // Other roles → only 1 branch
            branchCode = fetchBranchCodeByRole(role, email);
            branchCodes = List.of(branchCode);
        }

        // ✅ Base fetch by branch
        List<AdmissionForm> admissions = admissionRepository.findByBranchCodeInAndDateIsNotNull(branchCodes);

        // 👇 Extra filter if role = STAFF
        if ("STAFF".equalsIgnoreCase(role)) {
            admissions = admissions.stream()
                    .filter(a -> email.equalsIgnoreCase(a.getCreatedByEmail()))
                    .toList();
        }

        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate last7 = today.minusDays(7);
        LocalDate last30 = today.minusDays(30);
        LocalDate last365 = today.minusDays(365);

        // Pre-filtered lists
        List<AdmissionForm> todayAdmissions = new ArrayList<>();
        List<AdmissionForm> last7DaysAdmissions = new ArrayList<>();
        List<AdmissionForm> last30DaysAdmissions = new ArrayList<>();
        List<AdmissionForm> last365DaysAdmissions = new ArrayList<>();

        for (AdmissionForm a : admissions) {
            LocalDate date = a.getDate();
            if (date == null) continue;

            if (date.isEqual(today)) todayAdmissions.add(a);
            if (date.isAfter(last7)) last7DaysAdmissions.add(a);
            if (date.isAfter(last30)) last30DaysAdmissions.add(a);
            if (date.isAfter(last365)) last365DaysAdmissions.add(a);
        }

        // Stats calculations
        stats.put("todayCount", (long) todayAdmissions.size());
        stats.put("todayRevenue", todayAdmissions.stream().mapToDouble(this::getPaidFees).sum());
        stats.put("todayPendingFees", todayAdmissions.stream().mapToDouble(this::getPendingFees).sum());

        stats.put("last7DaysCount", (long) last7DaysAdmissions.size());
        stats.put("last7DaysRevenue", last7DaysAdmissions.stream().mapToDouble(this::getPaidFees).sum());
        stats.put("last7DaysPendingFees", last7DaysAdmissions.stream().mapToDouble(this::getPendingFees).sum());

        stats.put("last30DaysCount", (long) last30DaysAdmissions.size());
        stats.put("last30DaysRevenue", last30DaysAdmissions.stream().mapToDouble(this::getPaidFees).sum());
        stats.put("last30DaysPendingFees", last30DaysAdmissions.stream().mapToDouble(this::getPendingFees).sum());

        stats.put("last365DaysCount", (long) last365DaysAdmissions.size());
        stats.put("last365DaysRevenue", last365DaysAdmissions.stream().mapToDouble(this::getPaidFees).sum());
        stats.put("last365DaysPendingFees", last365DaysAdmissions.stream().mapToDouble(this::getPendingFees).sum());

        stats.put("totalCount", (long) admissions.size());
        stats.put("totalRevenue", admissions.stream().mapToDouble(this::getPaidFees).sum());
        stats.put("totalPendingFees", admissions.stream().mapToDouble(this::getPendingFees).sum());

        return stats;
    }

    // Helper methods
    private double getPaidFees(AdmissionForm a) {
        return a.getPaidFees() != null ? a.getPaidFees() : 0.0;
    }

    private double getPendingFees(AdmissionForm a) {
        return a.getPendingFees() != null ? a.getPendingFees() : 0.0;
    }



    public Map<String, Map<String, Object>> getDailyAdmissionStats(
            String role, String email, int month, int year, @Nullable String branchCode) {

        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view stats");
        }

        List<String> branchCodes;

        if ("superadmin".equalsIgnoreCase(role)) {
            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (response == null || response.isEmpty()) {
                return new TreeMap<>();
            }

            List<String> allBranchCodes = new ArrayList<>(response.values());

            if (branchCode == null || branchCode.isBlank()) {
                branchCodes = allBranchCodes;
            } else {
                if (!allBranchCodes.contains(branchCode)) {
                    return new TreeMap<>();
                }
                branchCodes = List.of(branchCode);
            }
        } else {
            branchCode = fetchBranchCodeByRole(role, email);
            branchCodes = List.of(branchCode);
        }

        // ✅ Get first and last date of the month
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // ✅ Fetch admissions in single query
        List<AdmissionForm> admissions = admissionRepository
                .findByBranchCodeInAndDateBetween(branchCodes, startDate, endDate);

        // ✅ Staff role filtering (email based)
        if ("STAFF".equalsIgnoreCase(role)) {
            admissions = admissions.stream()
                    .filter(a -> email.equalsIgnoreCase(a.getCreatedByEmail()))
                    .toList();
        }

        Map<String, Map<String, Object>> result = new TreeMap<>();

        for (AdmissionForm a : admissions) {
            LocalDate date = a.getDate();
            if (date == null) continue;

            String dateStr = date.toString();

            Map<String, Object> stats = result.computeIfAbsent(dateStr, d -> {
                Map<String, Object> map = new HashMap<>();
                map.put("count", 0L);
                map.put("revenue", 0.0);
                return map;
            });

            long count = (long) stats.get("count") + 1;
            double revenue = (double) stats.get("revenue") +
                    (a.getPaidFees() != null ? a.getPaidFees() : 0.0);

            stats.put("count", count);
            stats.put("revenue", revenue);
        }

        return result;
    }


    @Override
//    @Cacheable(
//            value = "monthlyAdmissionStatsCache",
//            key = "'monthlyStats_' + #role + '_' + #email + '_' + #year + '_' + (#branchCode != null ? #branchCode : 'all')"
//    )
    public Map<String, Map<String, Object>> getMonthlyAdmissionStats(String role, String email, int year, @Nullable String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view stats");
        }

        List<String> branchCodes;

        if ("superadmin".equalsIgnoreCase(role)) {
            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (response == null || response.isEmpty()) {
                return new TreeMap<>();
            }

            List<String> allBranchCodes = new ArrayList<>(response.values());

            if (branchCode == null || branchCode.isBlank()) {
                branchCodes = allBranchCodes;
            } else {
                if (!allBranchCodes.contains(branchCode)) {
                    return new TreeMap<>();
                }
                branchCodes = List.of(branchCode);
            }
        } else {
            branchCode = fetchBranchCodeByRole(role, email);
            branchCodes = List.of(branchCode);
        }

        // Get date range for full year
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // ✅ Optimized DB call
        List<AdmissionForm> admissions = admissionRepository
                .findByBranchCodeInAndDateBetween(branchCodes, startDate, endDate);

        // 👇 Extra filter if role = STAFF
        if ("STAFF".equalsIgnoreCase(role)) {
            admissions = admissions.stream()
                    .filter(a -> email.equalsIgnoreCase(a.getCreatedByEmail()))
                    .toList();
        }

        // Prepare result with default values for each month
        Map<String, Map<String, Object>> result = new TreeMap<>();
        for (int month = 1; month <= 12; month++) {
            String monthKey = String.format("%02d", month);
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("count", 0L);
            defaultStats.put("revenue", 0.0);
            result.put(monthKey, defaultStats);
        }

        // Populate real data
        for (AdmissionForm a : admissions) {
            if (a.getDate() == null) continue;

            String monthKey = String.format("%02d", a.getDate().getMonthValue());
            Map<String, Object> stats = result.get(monthKey);

            long updatedCount = (long) stats.get("count") + 1;
            double updatedRevenue = (double) stats.get("revenue") +
                    (a.getPaidFees() != null ? a.getPaidFees() : 0.0);

            stats.put("count", updatedCount);
            stats.put("revenue", updatedRevenue);
        }

        return result;
    }



    @Override
//    @Cacheable(
//            value = "twoYearComparisonStatsCache",
//            key = "'compareStats_' + #role + '_' + #email + '_' + #year1 + '_' + #year2 + '_' + (#branchCode != null ? #branchCode : 'all')"
//    )
    public Map<String, Map<String, Object>> getTwoYearComparisonStats(String role, String email, int year1, int year2, @Nullable String branchCode) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view stats");
        }

        List<String> branchCodes;

        if ("superadmin".equalsIgnoreCase(role)) {
            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (response == null || response.isEmpty()) {
                return new LinkedHashMap<>();
            }

            List<String> allBranchCodes = new ArrayList<>(response.values());

            if (branchCode == null || branchCode.isBlank()) {
                branchCodes = allBranchCodes;
            } else {
                if (!allBranchCodes.contains(branchCode)) {
                    return new LinkedHashMap<>();
                }
                branchCodes = List.of(branchCode);
            }

        } else {
            branchCode = fetchBranchCodeByRole(role, email);
            branchCodes = List.of(branchCode);
        }

        // Define date range to fetch both years in one query
        LocalDate startDate = LocalDate.of(Math.min(year1, year2), 1, 1);
        LocalDate endDate = LocalDate.of(Math.max(year1, year2), 12, 31);

        // ✅ Optimized DB call
        List<AdmissionForm> admissions = admissionRepository
                .findByBranchCodeInAndDateBetween(branchCodes, startDate, endDate);

        // 👇 Extra filter if role = STAFF
        if ("STAFF".equalsIgnoreCase(role)) {
            admissions = admissions.stream()
                    .filter(a -> email.equalsIgnoreCase(a.getCreatedByEmail()))
                    .toList();
        }

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        String[] months = {
                "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
        };

        // Initialize structure
        for (int i = 0; i < 12; i++) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("admissionsCount_" + year1, 0L);
            stats.put("admissionsCount_" + year2, 0L);
            stats.put("revenue_" + year1, 0.0);
            stats.put("revenue_" + year2, 0.0);
            result.put(months[i], stats);
        }

        // Populate data
        for (AdmissionForm a : admissions) {
            if (a.getDate() == null) continue;

            int year = a.getDate().getYear();
            if (year != year1 && year != year2) continue;

            String monthKey = months[a.getDate().getMonthValue() - 1];
            Map<String, Object> stats = result.get(monthKey);

            String countKey = "admissionsCount_" + year;
            String revenueKey = "revenue_" + year;

            long newCount = (long) stats.get(countKey) + 1;
            double newRevenue = (double) stats.get(revenueKey) +
                    (a.getPaidFees() != null ? a.getPaidFees() : 0.0);

            stats.put(countKey, newCount);
            stats.put(revenueKey, newRevenue);
        }

        return result;
    }


    @Override
//    @Cacheable(
//            value = "courseStatsCache",
//            key = "'courseStats_' + #role + '_' + #email + '_' + #year + '_' + #month + '_' + (#branchCode != null ? #branchCode : 'all')"
//    )
    public Map<String, Map<String, Object>> getAdmissionCountAndRevenueByCourseName(
            int year, String month, String role, String email,
            LocalDate startDate, LocalDate endDate,
            @Nullable String branchCode) {

        // Check permission
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("No permission to view admission data");
        }

        List<AdmissionForm> admissions;

        // Super Admin logic
        if ("superadmin".equalsIgnoreCase(role)) {
            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (response == null || response.isEmpty()) return new HashMap<>();

            List<String> allBranchCodes = new ArrayList<>(response.values());

            if (branchCode == null || branchCode.isBlank()) {
                admissions = admissionRepository.findAll().stream()
                        .filter(a -> a.getBranchCode() != null && allBranchCodes.contains(a.getBranchCode()))
                        .toList();
            } else {
                if (!allBranchCodes.contains(branchCode)) return new HashMap<>();
                admissions = admissionRepository.findAll().stream()
                        .filter(a -> branchCode.equals(a.getBranchCode()))
                        .toList();
            }

        } else {
            // Branch-based role (e.g., admin, teacher)
            String code = fetchBranchCodeByRole(role, email);
            admissions = admissionRepository.findAll().stream()
                    .filter(a -> code.equals(a.getBranchCode()))
                    .toList();
            if ("STAFF".equalsIgnoreCase(role)) {
                admissions = admissions.stream()
                        .filter(a -> email.equalsIgnoreCase(a.getCreatedByEmail()))
                        .toList();
            }
        }

        // Filter admissions by date
        List<AdmissionForm> filteredAdmissions;
        if ("all".equalsIgnoreCase(month)) {
            filteredAdmissions = admissions.stream()
                    .filter(a -> a.getDate() != null && a.getDate().getYear() == year)
                    .toList();
        } else if ("custom".equalsIgnoreCase(month) && startDate != null && endDate != null) {
            filteredAdmissions = admissions.stream()
                    .filter(a -> a.getDate() != null &&
                            !a.getDate().isBefore(startDate) &&
                            !a.getDate().isAfter(endDate))
                    .toList();
        } else {
            int numericMonth;
            try {
                numericMonth = Integer.parseInt(month);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid month format: " + month);
            }

            filteredAdmissions = admissions.stream()
                    .filter(a -> a.getDate() != null &&
                            a.getDate().getYear() == year &&
                            a.getDate().getMonthValue() == numericMonth)
                    .toList();
        }



        // Group and calculate stats by course name
        Map<String, Map<String, Object>> courseStats = new HashMap<>();

        for (AdmissionForm admission : filteredAdmissions) {
            String courseName = admission.getCoursename();
            if (courseName == null || courseName.trim().isEmpty()) continue;

            Map<String, Object> stats = courseStats.getOrDefault(courseName, new HashMap<>());

            long count = (long) stats.getOrDefault("admissionsCount", 0L) + 1;
            double revenue = (double) stats.getOrDefault("revenue", 0.0) +
                    Optional.ofNullable(admission.getPaidFees()).orElse(0.0);

            stats.put("admissionsCount", count);
            stats.put("revenue", revenue);
            courseStats.put(courseName, stats);
        }

        return courseStats;
    }

    @Override
//    @Cacheable(
//            value = "sourceStatsCache",
//            key = "'sourceStats_' + #role + '_' + #email + '_' + (#branchCode != null ? #branchCode : 'all')"
//    )
    public Map<String, Long> getAdmissionsCountBySourceBy(String role, String email, @Nullable String branchCode, String timeFrame, @Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view this data.");
        }

        List<AdmissionForm> admissions;

        if ("superadmin".equalsIgnoreCase(role)) {
            // Fetch all branches assigned to this SuperAdmin
            Map<String, String> branches = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (branches == null || branches.isEmpty()) return Map.of();

            List<String> branchCodes = new ArrayList<>(branches.values());

            if (branchCode != null && !branchCode.isBlank()) {
                if (!branchCodes.contains(branchCode)) return Map.of(); // unauthorized access
                admissions = admissionRepository.findAll().stream()
                        .filter(a -> branchCode.equals(a.getBranchCode()))
                        .toList();
            } else {
                admissions = admissionRepository.findAll().stream()
                        .filter(a -> branchCodes.contains(a.getBranchCode()))
                        .toList();
            }
        } else {
            String resolvedBranchCode = fetchBranchCodeByRole(role, email);
            admissions = admissionRepository.findAll().stream()
                    .filter(a -> resolvedBranchCode.equals(a.getBranchCode()))
                    .toList();
            if ("STAFF".equalsIgnoreCase(role)) {
                admissions = admissions.stream()
                        .filter(a -> email.equalsIgnoreCase(a.getCreatedByEmail()))
                        .toList();
            }
        }

        LocalDate today = LocalDate.now();
        switch (timeFrame.toLowerCase()) {
            case "today" -> admissions = admissions.stream()
                    .filter(a -> today.equals(a.getDate()))
                    .toList();

            case "7days" -> admissions = admissions.stream()
                    .filter(a -> !a.getDate().isBefore(today.minusDays(6)))
                    .toList();

            case "30days" -> admissions = admissions.stream()
                    .filter(a -> !a.getDate().isBefore(today.minusDays(29)))
                    .toList();

            case "365days" -> admissions = admissions.stream()
                    .filter(a -> !a.getDate().isBefore(today.minusDays(364)))
                    .toList();

            case "custom" -> {
                if (startDate != null && endDate != null) {
                    admissions = admissions.stream()
                            .filter(a -> !a.getDate().isBefore(startDate) &&
                                    !a.getDate().isAfter(endDate))
                            .toList();
                }
            }

            case "all" -> {

            }

            default -> throw new IllegalArgumentException("Invalid timeFrame: " + timeFrame);
        }

        return admissions.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSourceBy() == null || a.getSourceBy().isBlank() ? "Unknown" : a.getSourceBy(),
                        Collectors.counting()
                ));
    }


    @Override
//    @Cacheable(value = "admissionsByTeacherEmail", key = "#teacherEmail + '-' + #role + '-' + #email", unless = "#result == null or #result.isEmpty()")
    public List<AdmissionForm> getAdmissionsByTeacherEmail(String email, String role, String branchCode) {
        if (!"TEACHER".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Only TEACHER role can access this API");
        }

        List<AdmissionClassRoom> classrooms = classRoomRepository.findByTeachersEmailAndBranchCode(email, branchCode);
        if (classrooms.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> classroomIds = classrooms.stream()
                .map(AdmissionClassRoom::getId)
                .toList();

        return admissionRepository.findByAdmissionClassRoomIdInAndBranchCode(classroomIds, branchCode);
    }

    @Override
    public AdmissionLoginResponse login(AdmissionLoginRequest request) {
        AdmissionForm admission = admissionRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), admission.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(admission.getEmail());

        Long classroomId = null;
        if (admission.getAdmissionClassRoom() != null) {
            classroomId = admission.getAdmissionClassRoom().getId();
        }

        return new AdmissionLoginResponse(
                admission.getId(),
                admission.getName(),
                admission.getEmail(),
                token,
                "Login successful",
                admission.getBranchCode(),
                classroomId // ✅ include classroom id in response
        );
    }



    @Override
    public String sendOtpToEmail(String email) {
        AdmissionForm form = admissionRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String otp = String.valueOf((int) (Math.random() * 900000 + 100000));
        otpService.saveOtp(email, otp);
        emailService.sendOtpEmail(email, otp); // This method sends email

        return "OTP sent to your email";
    }

    @Override
    public String resetPassword(String email, String otp, String newPassword) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        AdmissionForm form = admissionRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        form.setPassword(passwordEncoder.encode(newPassword));
        admissionRepository.save(form);

        return "Password reset successfully";
    }

    @Override
    public AdmissionForm getAdmissionById(Long id) {
        Optional<AdmissionForm> optionalAdmission = admissionRepository.findById(id);
        return optionalAdmission.orElseThrow(() -> new RuntimeException("Admission not found with id: " + id));
    }


    @Override
    public AdmissionForm getAdmissionByClassroomRollNoAndBranchCode(Long classroomId, Integer rollNo, String branchCode) {
        return admissionRepository
                .findByAdmissionClassRoom_IdAndRollNoAndBranchCode(classroomId, rollNo, branchCode)
                .orElseThrow(() -> new RuntimeException("Admission not found for given parameters."));
    }

    @Override
    public List<String> uploadAdmissionsFromCsv(MultipartFile file, String role, String email) {
        if (!hasPermission(role, email, "POST")) {
            throw new AccessDeniedException("No permission to upload AdmissionForm CSV");
        }

        List<AdmissionForm> admissionList = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        String branchCode = fetchBranchCodeByRole(role, email);
        int nextRegCounter = getNextRegistrationCounter();
        int nextRollNo = getNextRollNumber();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        try (
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)
        ) {
            Map<String, String> values;
            int rowNumber = 1;

            while ((values = csvReader.readMap()) != null) {
                AdmissionForm form = AdmissionForm.builder()
                        .name(values.get("Name"))
                        .mobile1(values.get("Mobile1"))
                        .date(safeParseDate(values.get("Date")) != null ? safeParseDate(values.get("Date")) : LocalDate.now())
                        .status(values.getOrDefault("Status", "Active"))
                        .coursename(values.get("Course"))
                        .duration(values.get("Duration"))
                        .email(values.get("Email"))
                        .mobile2(values.get("Mobile2"))
                        .totalFees(parseDouble(values.get("TotalFees")))
                        .remark(values.get("Remark"))
                        .dueDate(safeParseDate(values.get("DueDate")))
                        .mediumName(values.get("Medium"))
                        .paymentMethod(values.get("PaymentMethod"))
                        .paymentMode(values.get("PaymentMode"))
                        .paidFees(parseDouble(values.get("PaidFees")))
                        .sourceBy(values.get("SourceBy"))
                        .currentAddress(values.get("CurrentAddress"))
                        .permanentAddress(values.get("PermanentAddress"))
                        .academicYear(values.get("AcademicYear"))
                        .gender(values.get("Gender"))
                        .dob(safeParseDate(values.get("DOB")))
                        .reference(values.get("Reference"))
                        .aadhaarCardNo(values.get("AadhaarCardNo"))
                        .createdByEmail(email)
                        .role(role)
                        .branchCode(branchCode)
                        .registrationNo(LocalDate.now().getYear() + String.format("%04d", nextRegCounter++))
                        .rollNo(nextRollNo++)
                        .build();

                // Encode password
                String rawPassword = values.get("Password");
                form.setPassword(rawPassword != null && !rawPassword.trim().isEmpty()
                        ? passwordEncoder.encode(rawPassword.trim())
                        : null);

                // Expiredate + pending fees
                calculateExpireDate(form);
                double total = form.getTotalFees() != null ? form.getTotalFees() : 0.0;
                double paid = form.getPaidFees() != null ? form.getPaidFees() : 0.0;
                form.setPendingFees(total - paid);

                // ✅ Validate before adding
                Set<ConstraintViolation<AdmissionForm>> violations = validator.validate(form);
                if (!violations.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder();
                    for (ConstraintViolation<AdmissionForm> v : violations) {
                        errorMsg.append(v.getPropertyPath()).append(" ").append(v.getMessage()).append("; ");
                    }
                    errorMessages.add("Row " + rowNumber + ": " + errorMsg.toString());
                }

                admissionList.add(form);
                rowNumber++;
            }

            if (!errorMessages.isEmpty()) {
                return errorMessages;
            }

            // Otherwise save all rows
            admissionRepository.saveAll(admissionList);
            return List.of("CSV uploaded successfully with " + admissionList.size() + " records.");

        } catch (Exception e) {
            throw new RuntimeException("CSV parse failed: " + e.getMessage(), e);
        }
    }


    private void calculateExpireDate(AdmissionForm form) {
        if (form.getDate() == null || form.getDuration() == null) {
            return; // Nothing to do if either is null
        }

        try {
            String durationStr = form.getDuration().toLowerCase().trim();
            int number = Integer.parseInt(durationStr.replaceAll("[^0-9]", ""));

            if (durationStr.contains("year")) {
                form.setExpiredate(form.getDate().plusYears(number));
            } else if (durationStr.contains("month")) {
                form.setExpiredate(form.getDate().plusMonths(number));
            } else if (durationStr.contains("day")) {
                form.setExpiredate(form.getDate().plusDays(number));
            } else {
                System.out.println("Duration unit not recognized, expiredate not set: " + form.getDuration());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid duration number format: " + form.getDuration());
        }
    }


    private LocalDate safeParseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        String trimmed = value.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (Exception ignored) {}
        }

        System.out.println("Unrecognized date format: " + trimmed);
        return null;
    }

    private Double parseDouble(String value) {
        try {
            return (value != null && !value.trim().isEmpty()) ? Double.parseDouble(value.trim()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private int getNextRegistrationCounter() {
        Optional<AdmissionForm> last = admissionRepository.findTopByOrderByIdDesc();
        return last.map(l -> {
            String reg = l.getRegistrationNo();
            return (reg != null && reg.length() >= 8) ? Integer.parseInt(reg.substring(4)) + 1 : 1;
        }).orElse(1);
    }

    private int getNextRollNumber() {
        Optional<AdmissionForm> last = admissionRepository.findTopByOrderByIdDesc();
        return last.map(l -> (l.getRollNo() != null ? l.getRollNo() + 1 : 1)).orElse(1);
    }



    @Override
//    @CacheEvict(value = "admissionsByClassroomId", key = "#classroomId + '-' + #role + '-' + #email")
    public void removeStudentsFromClassroom(Long classroomId, List<Long> admissionFormIds, String role, String email) {
        if (!hasPermission(role, email, "PUT")) {
            throw new AccessDeniedException("You do not have permission to remove students from the classroom.");
        }

        AdmissionClassRoom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Classroom not found"));

        List<AdmissionForm> admissions = admissionRepository.findAllById(admissionFormIds);

        for (AdmissionForm admission : admissions) {
            if (admission.getAdmissionClassRoom() != null &&
                    admission.getAdmissionClassRoom().getId().equals(classroomId)) {
                admission.setAdmissionClassRoom(null);
            }
        }

        admissionRepository.saveAll(admissions);
    }

    @Override
    public ParentLoginResponse parentLogin(ParentLoginRequest request) {
        AdmissionForm admission = admissionRepository.findByParentEmail(request.getParentEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), admission.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(admission.getParentEmail());

        return new ParentLoginResponse(
                admission.getId(),
                admission.getName(),         // ✅ Student Name
                admission.getParentEmail(),
                token,
                "Parent login successful",
                admission.getBranchCode()
        );
    }

    @Override
    public String sendOtpToParentEmail(String parentEmail) {
        if (parentEmail == null || parentEmail.isBlank()) {
            throw new RuntimeException("Parent email is required");
        }

        AdmissionForm admission = admissionRepository.findByParentEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("Parent email not found"));

        String otp = String.valueOf((int) (Math.random() * 900000 + 100000));
        otpService.saveOtp(parentEmail, otp);
        emailService.sendOtpEmail(parentEmail, otp);

        return "OTP sent to parent email";
    }


    @Override
    public String resetParentPassword(String parentEmail, String otp, String newPassword) {
        if (!otpService.verifyOtp(parentEmail, otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        AdmissionForm admission = admissionRepository.findByParentEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("Parent not found"));

        admission.setPassword(passwordEncoder.encode(newPassword));
        admissionRepository.save(admission);

        return "Parent password reset successfully";
    }

    @Override
    public Map<String, Long> getAdmissionsCountByStaffInBranch(String role, String email, String branchCode) {
        // ✅ Step 1: Permission check
        if (!hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You do not have permission to view this data.");
        }

        // ✅ Step 2: Superadmin specific validation (optional but safer)
        if ("SUPERADMIN".equalsIgnoreCase(role)) {
            Map<String, String> branches = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getBranchCodesByinstituteEmail")
                            .queryParam("instituteEmail", email)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (branches == null || !branches.containsValue(branchCode)) {
                throw new AccessDeniedException("This branch does not belong to you");
            }
        } else {
            // ✅ Step 3: Non-superadmin — branch/department/staff must match the branch they belong to
            String resolvedBranchCode = fetchBranchCodeByRole(role, email);
            if (!resolvedBranchCode.equals(branchCode)) {
                throw new AccessDeniedException("You do not belong to this branch");
            }
        }

        // ✅ Step 4: Fetch all admissions from this branch
        List<AdmissionForm> admissions = admissionRepository.findAll().stream()
                .filter(a -> branchCode.equals(a.getBranchCode()))
                .toList();

        // ✅ Step 5: Group by createdByEmail
        return admissions.stream()
                .filter(a -> a.getCreatedByEmail() != null && !a.getCreatedByEmail().isBlank())
                .collect(Collectors.groupingBy(
                        AdmissionForm::getCreatedByEmail,
                        Collectors.counting()
                ));
    }


}