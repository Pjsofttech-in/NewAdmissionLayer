package com.newadmission.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BranchAddressDTO
{
    private String branchName;
    private String branchEmail;
    private String contact;
    private String branchImage;
    private String address;
    private String city;
    private String district;
    private String state;
    private String country;
    private Integer pincode;
}
