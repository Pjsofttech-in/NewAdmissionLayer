package com.newadmission.Pegination;

import com.newadmission.DTO.AdmissionFilterRequest;
import com.newadmission.Entity.AdmissionForm;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class AdmissionSpecification {

    public static Specification<AdmissionForm> withDynamicFilters(AdmissionFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getBranchCode() != null && !request.getBranchCode().isEmpty()) {
                predicates.add(cb.equal(root.get("branchCode"), request.getBranchCode()));
            }

            if (request.getName() != null && !request.getName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + request.getName().toLowerCase() + "%"));
            }

            if (request.getMobile1() != null && !request.getMobile1().isEmpty()) {
                predicates.add(cb.like(root.get("mobile1"), "%" + request.getMobile1() + "%"));
            }


            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), request.getStatus().toLowerCase()));
            }

            if (request.getCoursename() != null && !request.getCoursename().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("coursename")), request.getCoursename().toLowerCase()));
            }

            if (request.getGender() != null && !request.getGender().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("gender")), request.getGender().toLowerCase()));
            }

            if (request.getAcademicYear() != null && !request.getAcademicYear().isEmpty()) {
                predicates.add(cb.equal(root.get("academicYear"), request.getAcademicYear()));
            }

            if (request.getSourceBy() != null && !request.getSourceBy().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("sourceBy")), request.getSourceBy().toLowerCase()));
            }

            if (request.getGuideName() != null && !request.getGuideName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("guideName")), "%" + request.getGuideName().toLowerCase() + "%"));
            }
            if (request.getMediumName() != null && !request.getMediumName().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("mediumName")), request.getMediumName().toLowerCase()));
            }
            if(request.getPaymentMethod() != null && !request.getPaymentMethod().isEmpty()){
                predicates.add(cb.equal(cb.lower(root.get("paymentMethod")),request.getPaymentMethod().toLowerCase()));
            }

            if(request.getPaymentMode() != null && !request.getPaymentMode().isEmpty()){
                predicates.add(cb.equal(cb.lower(root.get("paymentMode")),request.getPaymentMode().toLowerCase()));
            }

            if(request.getReference() != null && !request.getReference().isEmpty()){
                predicates.add(cb.equal(cb.lower(root.get("reference")),request.getReference().toLowerCase()));
            }

            // 🔹 Join with Installment for installmentCount and month
            Join<Object, Object> installmentJoin = root.join("installments", JoinType.LEFT);

            if (request.getInstallmentCount() != null && !request.getInstallmentCount().isEmpty()) {
                predicates.add(cb.equal(cb.lower(installmentJoin.get("installmentCount")),
                        request.getInstallmentCount().toLowerCase()));
            }

            if (request.getMonth() != null && !request.getMonth().isEmpty()) {
                predicates.add(cb.equal(cb.lower(installmentJoin.get("month")),
                        request.getMonth().toLowerCase()));
            }

            if (request.getFilterType() != null && !request.getFilterType().isEmpty()) {
                LocalDate today = LocalDate.now();
                LocalDate fromDate;
                LocalDate toDate = today;

                switch (request.getFilterType().toLowerCase()) {
                    case "today" -> fromDate = today;
                    case "last7days" -> fromDate = today.minusDays(7);
                    case "last30days" -> fromDate = today.minusDays(30);
                    case "last365days" -> fromDate = today.minusDays(365);
                    case "custom" -> {
                        if (request.getStartDate() == null || request.getEndDate() == null) {
                            throw new IllegalArgumentException("Start date and end date must be provided for custom filter");
                        }
                        fromDate = request.getStartDate();
                        toDate = request.getEndDate();
                    }
                    default -> throw new IllegalArgumentException("Invalid filter type: " + request.getFilterType());
                }

                predicates.add(cb.between(root.get("date"), fromDate, toDate));
            }

            // 🔹 createdByEmail filter
            if (request.getRole() != null && "STAFF".equalsIgnoreCase(request.getRole())) {
                // STAFF can see only their own admissions
                predicates.add(cb.equal(root.get("createdByEmail"), request.getEmail()));
            } else if (request.getCreatedByEmail() != null && !request.getCreatedByEmail().isEmpty()) {
                // Admin / other roles can filter by createdByEmail
                predicates.add(cb.equal(root.get("createdByEmail"), request.getCreatedByEmail()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


}