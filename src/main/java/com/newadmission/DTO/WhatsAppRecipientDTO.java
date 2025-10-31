package com.newadmission.DTO;

import lombok.Data;

import java.util.List;

@Data
public class WhatsAppRecipientDTO {
    private String phone;
    private String templateId;
    private List<String> parameters;
}
