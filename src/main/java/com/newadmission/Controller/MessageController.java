package com.newadmission.Controller;

import com.newadmission.DTO.BulkWhatsAppRequest;
import com.newadmission.Service.GupshupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "https://pjsofttech.in")
@RestController
public class MessageController {

    private final GupshupService service;

    public MessageController(GupshupService service) {
        this.service = service;
    }

    @PostMapping("/sendwhatsapptemplate")
    public ResponseEntity<?> sendWhatsApp(@RequestBody BulkWhatsAppRequest request) {
        return ResponseEntity.ok(Map.of("results", service.sendWhatsAppTemplate(request)));
    }
}