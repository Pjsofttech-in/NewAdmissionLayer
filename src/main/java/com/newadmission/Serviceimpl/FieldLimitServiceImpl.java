//package com.newadmission.Serviceimpl;
//
//import com.newadmission.Entity.InquiryFieldLimit;
//import com.newadmission.Repository.FieldLimitRepository;
//import com.newadmission.Service.FieldLimitService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class FieldLimitServiceImpl implements FieldLimitService
//{
//
//    @Autowired
//    private FieldLimitRepository fieldLimitRepository;
//
//    @Override
//    public List<InquiryFieldLimit> saveFieldLimits(String instituteEmail, String entityName, List<String> enabledFields) {
//
//        List<InquiryFieldLimit> existing = fieldLimitRepository.findEnabledFieldsByInstituteEmailAndEntityName(instituteEmail, entityName);
//        fieldLimitRepository.deleteAll(existing);
//
//        List<InquiryFieldLimit> newLimits = enabledFields.stream()
//                .map(field -> {
//                    InquiryFieldLimit limit = new InquiryFieldLimit();
//                    limit.setInstituteEmail(instituteEmail);
//                    limit.setEntityName(entityName);
//                    limit.setFieldName(field);
//                    limit.setEnabled(true);
//                    return limit;
//                }).collect(Collectors.toList());
//
//
//        return fieldLimitRepository.saveAll(newLimits);
//    }
//
//    @Override
//    public List<String> getEnabledFields(String userEmail, String entityName) {
//        return fieldLimitRepository.findEnabledFieldsByInstituteEmailAndEntityName(userEmail, entityName).stream()
//                .filter(InquiryFieldLimit::isEnabled)
//                .map(InquiryFieldLimit::getFieldName)
//                .collect(Collectors.toList());
//    }
//}
