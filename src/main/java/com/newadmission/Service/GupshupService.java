package com.newadmission.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newadmission.DTO.BulkWhatsAppRequest;
import com.newadmission.DTO.WhatsAppRecipientDTO;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GupshupService {
    @Value("${gupshup.whatsapp.api-key}")
    private String whatsappApiKey;

    @Value("${gupshup.whatsapp.url}")
    private String whatsappUrl;

    @Value("${gupshup.source.whatsapp}")
    private String whatsappSource;

    @Value("${gupshup.app.name}")
    private String appName;

    @Value("${gupshup.sms.api-key}")
    private String smsApiKey;

    @Value("${gupshup.sms.url}")
    private String smsUrl;

    @Value("${gupshup.source.sms}")
    private String smsSource;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GupshupService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> sendWhatsAppTemplate(BulkWhatsAppRequest request) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (WhatsAppRecipientDTO recipient : request.getRecipients()) {
            try {
                Map<String, Object> templateMap = new HashMap<>();
                templateMap.put("id", recipient.getTemplateId());
                templateMap.put("params", recipient.getParameters());

                String templateJson = objectMapper.writeValueAsString(templateMap);

                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("source", whatsappSource);
                formData.add("destination", recipient.getPhone());
                formData.add("template", templateJson);

                if (appName != null && !appName.isBlank()) {
                    formData.add("src.name", appName);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.set("apikey", whatsappApiKey);

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(whatsappUrl, entity, String.class);

                results.add(Map.of(
                        "phone", recipient.getPhone(),
                        "status_code", response.getStatusCodeValue(),
                        "response", response.getBody()
                ));

                System.out.println("Response → " + response.getBody());

            } catch (Exception e) {
                results.add(Map.of(
                        "phone", recipient.getPhone(),
                        "error", e.getMessage()
                ));
            }
        }

        return results;
    }


    // ✅ New method to send SMS text
    public Map<String, Object> sendSmsText(String phoneNumber, String message) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("channel", "sms");
            formData.add("source", smsSource);
            formData.add("destination", phoneNumber);
            formData.add("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("apikey", smsApiKey);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(smsUrl, entity, String.class);

            return Map.of(
                    "phone", phoneNumber,
                    "status_code", response.getStatusCodeValue(),
                    "response", response.getBody()
            );

        } catch (Exception e) {
            return Map.of(
                    "phone", phoneNumber,
                    "error", e.getMessage()
            );
        }
    }
}
