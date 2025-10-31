package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionSourceBy;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionSourceByService;
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
public class AdmissionSourceController {

    @Autowired
    private AdmissionSourceByService sourceByService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/createSourceBy")
    public ResponseEntity<AdmissionSourceBy> createSourceBy(@RequestBody AdmissionSourceBy sourceBy,
                                                            @RequestParam String role,
                                                            @RequestParam String email) {
        return ResponseEntity.ok(sourceByService.createSourceBy(sourceBy, role, email));
    }

    @GetMapping("/getAllSourceBy")
    public ResponseEntity<List<AdmissionSourceBy>> getAllSourceBy(@RequestParam String role,
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

        return ResponseEntity.ok(sourceByService.getAllSourceBy(role, email, branchCode));
    }

    @GetMapping("/getSourceById/{id}")
    public ResponseEntity<AdmissionSourceBy> getSourceById(@PathVariable Long id,
                                                           @RequestParam String role,
                                                           @RequestParam String email) {
        return ResponseEntity.ok(sourceByService.getSourceById(id, role, email));
    }

    @PutMapping("/updateSourceBy/{id}")
    public ResponseEntity<AdmissionSourceBy> updateSourceBy(@PathVariable Long id,
                                                            @RequestBody AdmissionSourceBy sourceBy,
                                                            @RequestParam String role,
                                                            @RequestParam String email) {
        return ResponseEntity.ok(sourceByService.updateSourceBy(id, sourceBy, role, email));
    }

    @DeleteMapping("/deleteSourceBy/{id}")
    public ResponseEntity<String> deleteSourceBy(@PathVariable Long id,
                                                 @RequestParam String role,
                                                 @RequestParam String email) {
        sourceByService.deleteSourceBy(id, role, email);
        return ResponseEntity.ok("Source by deleted successfully");
    }
}