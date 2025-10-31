package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionCourse;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionCourseService;
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
public class AdmissionCourseController {

    @Autowired
    private AdmissionCourseService service;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/createCourse")
    public ResponseEntity<AdmissionCourse> createCourse(@RequestBody AdmissionCourse course,
                                                        @RequestParam String role,
                                                        @RequestParam String email) {
        return ResponseEntity.ok(service.createCourse(course, role, email));
    }

    @GetMapping("/getAllCourses")
    public ResponseEntity<List<AdmissionCourse>> getAllCourses(@RequestParam String role,
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

        return ResponseEntity.ok(service.getAllCourses(role, email, branchCode));
    }


    @GetMapping("/getCourseById/{id}")
    public ResponseEntity<AdmissionCourse> getCourseById(@PathVariable Long id,
                                                         @RequestParam String role,
                                                         @RequestParam String email) {
        return ResponseEntity.ok(service.getCourseById(id, role, email));
    }

    @PutMapping("/updateCourse/{id}")
    public ResponseEntity<AdmissionCourse> updateCourse(@PathVariable Long id,
                                                        @RequestBody AdmissionCourse course,
                                                        @RequestParam String role,
                                                        @RequestParam String email) {
        return ResponseEntity.ok(service.updateCourse(id, course, role, email));
    }

    @DeleteMapping("/deleteCourse/{id}")
    public ResponseEntity<String> deleteCourse(@PathVariable Long id,
                                               @RequestParam String role,
                                               @RequestParam String email) {
        service.deleteCourse(id, role, email);
        return ResponseEntity.ok("Course deleted successfully");
    }
}