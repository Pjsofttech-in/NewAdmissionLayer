package com.newadmission.Controller;

import com.newadmission.Entity.AdmissionTask;
import com.newadmission.JWT.JwtUtil;
import com.newadmission.Service.AdmissionTaskService;
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
public class AdmissionTaskController {

    @Autowired
    private AdmissionTaskService admissionTaskService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/createTask")
    public ResponseEntity<AdmissionTask> createTask(@RequestBody AdmissionTask task,
                                                    @RequestParam String role,
                                                    @RequestParam String email) {
        AdmissionTask created = admissionTaskService.createTask(task, role, email);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/getAllTasks")
    public ResponseEntity<List<AdmissionTask>> getAllTasks(@RequestParam String role,
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

        List<AdmissionTask> tasks = admissionTaskService.getAllTasks(role, email, branchCode);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/getTaskById/{id}")
    public ResponseEntity<AdmissionTask> getTaskById(@PathVariable Long id,
                                                     @RequestParam String role,
                                                     @RequestParam String email) {
        AdmissionTask task = admissionTaskService.getTaskById(id, role, email);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/updateTask/{id}")
    public ResponseEntity<AdmissionTask> updateTask(@PathVariable Long id,
                                                    @RequestBody AdmissionTask task,
                                                    @RequestParam String role,
                                                    @RequestParam String email) {
        AdmissionTask updated = admissionTaskService.updateTask(id, task, role, email);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/deleteTask/{id}")
    public ResponseEntity<String> deleteTask(@PathVariable Long id,
                                             @RequestParam String role,
                                             @RequestParam String email) {
        admissionTaskService.deleteTask(id, role, email);
        return ResponseEntity.ok("Task deleted successfully");
    }
}
