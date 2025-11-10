package com.newadmission.Serviceimpl;

import com.newadmission.DTO.InstituteClientWrapperResponse;
import com.newadmission.DTO.InstituteLoginResponse;
import com.newadmission.JWT.LoginRequest;
import com.newadmission.JWT.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StaffService
{
    private final WebClient webClient;

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
                .bodyToMono(new ParameterizedTypeReference<Map<String, Boolean>>() {})
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
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

    }

    public List<String> getBranchCodesByInstituteEmail(String instituteEmail) {
        List<List<String>> nestedList = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getBranchCodesByinstituteEmail")
                        .queryParam("instituteEmail", instituteEmail)
                        .build())
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<List<String>>() {})
                .collectList()
                .block();
        return nestedList.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public Map<String, String> getBranchCodesWithNameByInstituteEmail(String instituteEmail) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getBranchCodesByinstituteEmail")
                        .queryParam("instituteEmail", instituteEmail)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
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

}
