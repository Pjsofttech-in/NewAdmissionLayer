package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionReference;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionReferenceService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class AdmissionReferenceController {

    @Autowired
    private AdmissionReferenceService admissionReferenceService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/createReference")
    public ResponseEntity<AdmissionReference> createReference(@RequestBody AdmissionReference reference,
                                                              @RequestParam String role,
                                                              @RequestParam String email) {
        return ResponseEntity.ok(admissionReferenceService.createReference(reference, role, email));
    }

    @GetMapping("/getAllReferences")
    public ResponseEntity<List<AdmissionReference>> getAllReferences(@RequestParam String role,
                                                                     @RequestParam(required = false) String email,
                                                                     @RequestParam(required = false) String branchCode,
                                                                     @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if ("USER".equalsIgnoreCase(role)) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Authorization token is required for USER role");
            }

            try {
                String token = authHeader.substring(7);
                Claims claims = jwtUtil.extractAllClaims(token);
                String encodedBranchCode = claims.get("branchCode", String.class);
                if (encodedBranchCode == null || encodedBranchCode.isEmpty()) {
                    throw new RuntimeException("Branch code not found in token");
                }

                byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedBranchCode);
                branchCode = new String(decodedBytes, StandardCharsets.UTF_8);
                email = null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode JWT token: " + e.getMessage(), e);
            }
        }

        return ResponseEntity.ok(admissionReferenceService.getAllReferences(role, email, branchCode));
    }


    @GetMapping("/getReferenceById/{id}")
    public ResponseEntity<AdmissionReference> getReferenceById(@PathVariable Long id,
                                                               @RequestParam String role,
                                                               @RequestParam String email) {
        return ResponseEntity.ok(admissionReferenceService.getReferenceById(id, role, email));
    }

    @PutMapping("/updateReference/{id}")
    public ResponseEntity<AdmissionReference> updateReference(@PathVariable Long id,
                                                              @RequestBody AdmissionReference reference,
                                                              @RequestParam String role,
                                                              @RequestParam String email) {
        return ResponseEntity.ok(admissionReferenceService.updateReference(id, reference, role, email));
    }

    @DeleteMapping("/deleteReference/{id}")
    public ResponseEntity<String> deleteReference(@PathVariable Long id,
                                                  @RequestParam String role,
                                                  @RequestParam String email) {
        admissionReferenceService.deleteReference(id, role, email);
        return ResponseEntity.ok("Reference deleted successfully");
    }
}