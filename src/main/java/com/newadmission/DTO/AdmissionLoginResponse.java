package com.newadmission.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionLoginResponse {
    private Long id;
    private String name;
    private String email;
    private String token;
    private String message;
    private String branchCode;
    private Long classroomId;
}
