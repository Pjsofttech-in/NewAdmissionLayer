package com.newadmission.DTO;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkResponse {

    private Long id;
    private String homework;
    private String imageUrl;
    private String subjectName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

    // Teacher info
    private String teacherName;
    private String teacherEmail;
    private String branchCode;
}