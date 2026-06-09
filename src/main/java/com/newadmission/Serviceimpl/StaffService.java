package com.newadmission.Serviceimpl;

import com.newadmission.DTO.*;
import com.newadmission.JWT.InternalJwtProvider;
import com.newadmission.JWT.LoginRequest;
import com.newadmission.JWT.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class StaffService {
    private final WebClient webClient;

    @Autowired
    InternalJwtProvider internalJwtProvider;

    @Autowired
    public StaffService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<LoginResponse> loginStaff(LoginRequest request) {
        return webClient.post()
                .uri("/stafflogin")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Login Failed: " + error)))
                )
                .bodyToMono(LoginResponse.class);
    }


    public Map<String, Boolean> getPermissionsByEmail(String email) {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/permissionForStaff")
                        .queryParam("staffEmail", email)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, token)  // pass it as-is
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Boolean>>() {
                })
                .block();
    }

    public boolean hasPermission(String role, String email, String action) {
        if ("USER".equalsIgnoreCase(role)) {
            return "GET".equalsIgnoreCase(action);
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
                Map<String, Boolean> perms = getPermissionsByEmail(email);
                yield switch (action.toUpperCase()) {
                    case "GET" -> Boolean.TRUE.equals(perms.get("cansGet"));
                    case "POST" -> Boolean.TRUE.equals(perms.get("cansPost"));
                    case "PUT" -> Boolean.TRUE.equals(perms.get("cansPut"));
                    case "DELETE" -> Boolean.TRUE.equals(perms.get("cansDelete"));
                    default -> false;
                };
            }
            case "DEPARTMENT" -> {
                Map<String, Object> perms = getCrudPermissionForDepartmentByEmail(email);
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

    public String fetchBranchCodeByRole(String role, String email) {
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

    public Map<String, Object> getCrudPermissionForDepartmentByEmail(String email) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/permissionForDepartment")
                        .queryParam("email", email)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, token)  // Pass token directly
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();

    }

    public List<String> getBranchCodesByInstituteEmail(String instituteEmail) {
        Map<String, String> branchMap = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getBranchCodesByinstituteEmail")
                        .queryParam("instituteEmail", instituteEmail)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .block();

        return branchMap != null
                ? new ArrayList<>(branchMap.values())
                : Collections.emptyList();
    }

    public Map<String, String> getBranchCodesWithNameByInstituteEmail(String instituteEmail) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getBranchCodesByinstituteEmail")
                        .queryParam("instituteEmail", instituteEmail)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .block();
    }

    public List<InstituteLoginResponse> getInstituteDetailsOnly(String email) {
        InstituteClientWrapperResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getLayerClientByClientEmail")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .bodyToMono(InstituteClientWrapperResponse.class)
                .block();

        return response != null ? response.getInstituteResponseDTOS() : Collections.emptyList();
    }

    public Mono<String> getInstituteEmailByBranchCode(String branchCode) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/instituteEmailByBranchCode")
                        .queryParam("branchCode", branchCode)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }


    public List<Map<String, Object>> getStaffNamesAndEmails(String branchCode) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getStaffbybranchCode")
                        .queryParam("branchCode", branchCode)
                        .build())
                .retrieve()
                .bodyToFlux(Map.class)
                .map(staff -> Map.of(
                        "name", staff.get("staffName"),
                        "email", staff.get("staffEmail")
                ))
                .collectList()
                .block();
    }

    public List<Map<String, Object>> getDepartmentByBranchCode(String branchCode) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getDepartmentByBranchcode")
                        .queryParam("branchCode", branchCode)
                        .build())
                .retrieve()
                .bodyToFlux(Map.class)
                .map(dept -> Map.of(
                        "name", dept.get("departmentName"),
                        "email", dept.get("departmentEmail")
                ))
                .collectList()
                .block();
    }

    public List<Map<String, Object>> getDepartmentForSuperAdmin(String branchCode, String instituteEmail) {
        if (branchCode != null && !branchCode.isEmpty()) {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getDepartmentByBranchcode")
                            .queryParam("branchCode", branchCode)
                            .build())
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .map(dept -> Map.of(
                            "name", dept.get("departmentName"),
                            "email", dept.get("departmentEmail")
                    ))
                    .collectList()
                    .block();
        } else {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getDepartmentByInstituteEmail")
                            .queryParam("instituteEmail", instituteEmail)
                            .build())
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .map(dept -> Map.of(
                            "name", dept.get("departmentName"),
                            "email", dept.get("departmentEmail"),
                            "branchCode", dept.get("branchCode")
                    ))
                    .collectList()
                    .block();
        }
    }

    public List<Map<String, Object>> getStaffInfoForSuperAdmin(String branchCode, String instituteEmail) {
        if (branchCode != null && !branchCode.isEmpty()) {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getStaffbybranchCode")
                            .queryParam("branchCode", branchCode)
                            .build())
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .map(staff -> Map.of(
                            "name", staff.get("staffName"),
                            "email", staff.get("staffEmail")
                    ))
                    .collectList()
                    .block();
        } else {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getStaffbyinstituteEmail")
                            .queryParam("instituteEmail", instituteEmail)
                            .build())
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .map(staff -> Map.of(
                            "name", staff.get("staffName"),
                            "email", staff.get("staffEmail"),
                            "branchCode", staff.get("branchCode")
                    ))
                    .collectList()
                    .block();
        }
    }

    public BranchAddressDTO getBranchAddressDetails(String branchCode) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/branchAddressDetailsByBranchCode")
                        .queryParam("branchCode", branchCode)
                        .build())
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> {
                                    System.err.println("SuperAdmin error: " + error);
                                    return Mono.error(new RuntimeException("Failed to fetch branch details"));
                                })
                )
                .bodyToMono(BranchAddressDTO.class)
                .block(); // ✅ convert reactive response to blocking for MVC app
    }


    public Mono<CreatedByResponseDTO> getCreatorByEmail(String email) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getNameByemail")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.empty()
                )
                .bodyToMono(CreatedByResponseDTO.class);
    }

    public Mono<String> getCreatedByName(String email) {
        return getCreatorByEmail(email)
                .map(CreatedByResponseDTO::getName)
                .defaultIfEmpty("Unknown");
    }


    public Map<String, Object> createOrder(String branchCode,
                                           String systemName,
                                           Long amount) {

        String internalToken = internalJwtProvider.generateInternalToken();

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/createOrder")
                        .queryParam("branchCode", branchCode)
                        .queryParam("systemName", systemName)
                        .queryParam("amount", amount)
                        .build())
                .header("Authorization", "Bearer " + internalToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
    }

    public Mono<Boolean> verifyPayment(RazorpayVerifyRequest request) {

        String internalToken = internalJwtProvider.generateInternalToken();

        return webClient.post()
                .uri("/verifyPayment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(err -> {
                                    System.out.println("Client verifyPayment failed: " + err);
                                    return Mono.error(new RuntimeException(err));
                                })

                )
                .bodyToMono(Boolean.class);
    }

    public Mono<String> sendFeeReminderViaWati(FeeReminderDTO request, String token) {
        return webClient.post()
                .uri("/watiTemplate/sendStudentFeeReminderMessage")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Login Failed: " + error)))
                )
                .bodyToMono(String.class);
    }

}
