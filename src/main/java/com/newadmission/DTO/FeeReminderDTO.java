package com.newadmission.DTO;

import lombok.Data;

@Data
public class FeeReminderDTO {
    private String dueDateStr;
    private Double collectAmount;
    private String studentName;
    private String studentPhoneNo;
}
