package com.newadmission.Controller;

import com.newadmission.DTO.InstituteLoginResponse;
import com.newadmission.JWT.LoginRequest;
import com.newadmission.JWT.LoginResponse;
import com.newadmission.Serviceimpl.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
public class StaffController
{
    @Autowired
    private StaffService staffLoginService;



    @PostMapping("/stafflogin")
    public Mono<ResponseEntity<LoginResponse>> loginStaff(@RequestBody LoginRequest request) {
        return staffLoginService.loginStaff(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    LoginResponse errorResponse = new LoginResponse();
                    errorResponse.setToken(null);
                    errorResponse.setData(Map.of("error", "Login failed: " + e.getMessage()));
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                });
    }

    @GetMapping("/permissionForStaff")
    public Map<String, Boolean> getPermission(@RequestParam String staffEmail) {
        return staffLoginService.getPermissionsByEmail(staffEmail);
    }


    @GetMapping("/permissionForDepartment")
    public ResponseEntity<Map<String, Object>> getDepartmentPermissions(@RequestParam String departmentEmail) {

        Map<String, Object> permissions = staffLoginService.getCrudPermissionForDepartmentByEmail(departmentEmail);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/getInstituteDetails")
    public ResponseEntity<List<InstituteLoginResponse>> getInstitute(@RequestParam String instituteEmail) {
        List<InstituteLoginResponse> response = staffLoginService.getInstituteDetailsOnly(instituteEmail);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/getBranchCodeByInstituteEmail")
    public ResponseEntity<Map<String, String>> getBranchCodesByInstituteEmail(@RequestParam String instituteEmail)
    {
        Map<String, String> branchMap = staffLoginService.getBranchCodesWithNameByInstituteEmail(instituteEmail);
        return ResponseEntity.ok(branchMap);
    }

}
