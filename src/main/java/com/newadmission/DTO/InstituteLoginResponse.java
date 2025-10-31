package com.newadmission.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstituteLoginResponse {

    private String instituteImage;
    private String instituteName;
    private Long phoneNumber;
    private String address;
    private String instituteEmail;

}