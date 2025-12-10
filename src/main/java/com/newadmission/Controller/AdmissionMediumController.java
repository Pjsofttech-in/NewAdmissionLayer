package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionMedium;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionMediumService;
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
public class AdmissionMediumController {

    @Autowired
    private AdmissionMediumService admissionMediumService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/createMedium")
    public ResponseEntity<AdmissionMedium> createMedium(@RequestBody AdmissionMedium medium,
                                                        @RequestParam String role,
                                                        @RequestParam String email) {
        AdmissionMedium created = admissionMediumService.createMedium(medium, role, email);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/getAllMediums")
    public ResponseEntity<List<AdmissionMedium>> getAllMediums(@RequestParam String role,
                                                               @RequestParam(required = false) String email,
                                                               @RequestParam(required = false) String branchCode,
                                                               @RequestHeader(value = "Authorization", required = false) String authHeader)
    {
        // For USER role, extract branchCode from JWT
        if ("USER".equalsIgnoreCase(role)) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Authorization token is required for USER role");
            }

            try {
                String token = authHeader.substring(7); // Remove "Bearer "
                Claims claims = jwtUtil.extractAllClaims(token);

                String encodedBranchCode = claims.get("branchCode", String.class);
                if (encodedBranchCode == null || encodedBranchCode.isEmpty()) {
                    throw new RuntimeException("Branch code not found in token");
                }

                byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedBranchCode);
                branchCode = new String(decodedBytes, StandardCharsets.UTF_8);
                email = null; // USER does not have email
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode JWT token: " + e.getMessage(), e);
            }
        }

        List<AdmissionMedium> mediums = admissionMediumService.getAllMediums(role, email, branchCode);
        return ResponseEntity.ok(mediums);
    }

    @GetMapping("/getMediumById/{id}")
    public ResponseEntity<AdmissionMedium> getMediumById(@PathVariable Long id,
                                                         @RequestParam String role,
                                                         @RequestParam String email) {
        AdmissionMedium medium = admissionMediumService.getMediumById(id, role, email);
        return ResponseEntity.ok(medium);
    }

    @PutMapping("/updateMedium/{id}")
    public ResponseEntity<AdmissionMedium> updateMedium(@PathVariable Long id,
                                                        @RequestBody AdmissionMedium medium,
                                                        @RequestParam String role,
                                                        @RequestParam String email) {
        AdmissionMedium updated = admissionMediumService.updateMedium(id, medium, role, email);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/deleteMedium/{id}")
    public ResponseEntity<String> deleteMedium(@PathVariable Long id,
                                               @RequestParam String role,
                                               @RequestParam String email) {
        admissionMediumService.deleteMedium(id, role, email);
        return ResponseEntity.ok("Medium deleted successfully");
    }
}