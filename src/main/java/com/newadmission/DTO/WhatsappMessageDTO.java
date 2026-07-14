package com.newadmission.DTO;

import lombok.*;

import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
@NoArgsConstructor   // Required by Jackson for deserialization
@AllArgsConstructor
@Data
public class WhatsappMessageDTO {
    private List<Long> studentIdList;
    private Long watiConfigId;
    private String branchCode;
    private Long template_id;
    private String templateName;
    private String whatsappNumber;
    private Map<String, Object> parameters;
}
