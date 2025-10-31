package com.newadmission.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubjectResultDto {
    private Long id;
    private String subjectName;
    private String examType;
    private String paperType;
    private int obtainedMarks;

    private int totalMarks;
    private int passingMarks;

    private String topicName;
}