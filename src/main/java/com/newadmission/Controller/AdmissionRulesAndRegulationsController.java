package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionRulesAndRegulations;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionRulesAndRegulationsService;
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
public class AdmissionRulesAndRegulationsController {

    @Autowired
    private AdmissionRulesAndRegulationsService service;

    @Autowired
    private JwtUtil jwtUtil;



    @PostMapping("/createRule")
    public ResponseEntity<AdmissionRulesAndRegulations> createRule(@RequestBody AdmissionRulesAndRegulations rule,
                                                                   @RequestParam String role,
                                                                   @RequestParam String email) {
        return ResponseEntity.ok(service.createRule(rule, role, email));
    }

    @GetMapping("/getAllRules")
    public ResponseEntity<List<AdmissionRulesAndRegulations>> getAllRules(@RequestParam String role,
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
                email = null; // USER साठी email लागू नाही
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode JWT token: " + e.getMessage(), e);
            }
        }

        return ResponseEntity.ok(service.getAllRules(role, email, branchCode));
    }


    @GetMapping("/getRuleById/{id}")
    public ResponseEntity<AdmissionRulesAndRegulations> getRuleById(@PathVariable Long id,
                                                                    @RequestParam String role,
                                                                    @RequestParam String email) {
        return ResponseEntity.ok(service.getRuleById(id, role, email));
    }

    @PutMapping("/updateRule/{id}")
    public ResponseEntity<AdmissionRulesAndRegulations> updateRule(@PathVariable Long id,
                                                                   @RequestBody AdmissionRulesAndRegulations rule,
                                                                   @RequestParam String role,
                                                                   @RequestParam String email) {
        return ResponseEntity.ok(service.updateRule(id, rule, role, email));
    }

    @DeleteMapping("/deleteRule/{id}")
    public ResponseEntity<String> deleteRule(@PathVariable Long id,
                                             @RequestParam String role,
                                             @RequestParam String email) {
        service.deleteRule(id, role, email);
        return ResponseEntity.ok("Rule deleted successfully");
    }
}