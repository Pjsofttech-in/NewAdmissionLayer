package com.newadmission.DTO;

import lombok.Data;

import java.util.List;

@Data
public class BulkWhatsAppRequest {
    private List<WhatsAppRecipientDTO> recipients;
}
