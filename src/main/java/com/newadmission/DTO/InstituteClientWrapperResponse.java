package com.newadmission.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InstituteClientWrapperResponse {
    private List<InstituteLoginResponse> instituteResponseDTOS;
}