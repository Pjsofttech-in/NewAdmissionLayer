package com.newadmission.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ParentLoginResponse {
    private Long id;
    private String studentName;
    private String parentEmail;
    private String token;
    private String message;
    private String branchCode;
}