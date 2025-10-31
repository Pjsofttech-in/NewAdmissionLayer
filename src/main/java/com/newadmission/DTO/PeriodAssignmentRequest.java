package com.newadmission.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeriodAssignmentRequest {
    private Integer periodId;
    private Integer teacherId;
    private Integer subjectId;
}