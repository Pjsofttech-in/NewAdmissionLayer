package com.newadmission.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentResultFilterRequest
{
    private String batchName;
    private String studentName;
    private String status;      // Pass / Fail
    private String academicYear;
    private String coursename;
    private String mediumName;
}
