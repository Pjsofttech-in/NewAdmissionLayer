package com.newadmission.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkEmailRequest {

    @NotEmpty(message = "Subject cannot be empty")
    private String subject;

    @NotEmpty(message = "Message cannot be empty")
    private String message;

    @NotNull(message = "ID list cannot be null")
    private List<Long> ids;
}