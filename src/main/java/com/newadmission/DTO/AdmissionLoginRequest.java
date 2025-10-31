package com.newadmission.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdmissionLoginRequest {
    private String email;
    private String password;
}
