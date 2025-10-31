//package com.newadmission.Controller;
//
//import com.newadmission.Entity.InquiryFieldLimit;
//import com.newadmission.Service.FieldLimitService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//public class FieldLimitController
//{
//    @Autowired
//    private FieldLimitService fieldLimitService;
//
//    @PostMapping("/save")
//    public ResponseEntity<List<InquiryFieldLimit>> saveFieldLimits(
//            @RequestParam String instituteEmail,
//            @RequestParam String entityName,
//            @RequestBody List<String> enabledFields) {
//
//        List<InquiryFieldLimit> savedLimits = fieldLimitService.saveFieldLimits(instituteEmail, entityName, enabledFields);
//        return ResponseEntity.ok(savedLimits);
//    }
//
//
//}
