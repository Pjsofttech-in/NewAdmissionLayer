package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionConductBy;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionConductByService;
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
public class AdmissionConductByController {

    @Autowired
    private AdmissionConductByService service;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/createConductBy")
    public ResponseEntity<AdmissionConductBy> createConductBy(@RequestBody AdmissionConductBy conductBy,
                                                              @RequestParam String role,
                                                              @RequestParam String email) {
        return ResponseEntity.ok(service.createConductBy(conductBy, role, email));
    }

    @GetMapping("/getAllConductBy")
    public ResponseEntity<List<AdmissionConductBy>> getAllConductBy(@RequestParam String role,
                                                                    @RequestParam(required = false) String email,
                                                                    @RequestParam(required = false) String branchCode,
                                                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Handle USER role using token
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

                email = null; // USER does not use email
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode JWT token: " + e.getMessage(), e);
            }
        }

        return ResponseEntity.ok(service.getAllConductBy(role, email, branchCode));
    }

    @GetMapping("/getConductById/{id}")
    public ResponseEntity<AdmissionConductBy> getConductById(@PathVariable Long id,
                                                             @RequestParam String role,
                                                             @RequestParam String email) {
        return ResponseEntity.ok(service.getConductById(id, role, email));
    }

    @PutMapping("/updateConductBy/{id}")
    public ResponseEntity<AdmissionConductBy> updateConductBy(@PathVariable Long id,
                                                              @RequestBody AdmissionConductBy conductBy,
                                                              @RequestParam String role,
                                                              @RequestParam String email) {
        return ResponseEntity.ok(service.updateConductBy(id, conductBy, role, email));
    }

    @DeleteMapping("/deleteConductBy/{id}")
    public ResponseEntity<String> deleteConductBy(@PathVariable Long id,
                                                  @RequestParam String role,
                                                  @RequestParam String email) {
        service.deleteConductBy(id, role, email);
        return ResponseEntity.ok("ConductBy deleted successfully");
    }
}