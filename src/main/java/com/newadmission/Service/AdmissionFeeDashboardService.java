package com.newadmission.Service;

import com.newadmission.DTO.FeeFilterSummaryDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdmissionFeeDashboardService {

    List<Map<String, Object>> getMonthlyRevenueForSpecificYear(int year, String role, String email);

    List<Map<String, Object>> getRevenueByYearRange(int startYear, int endYear, String role, String email);

    List<Map<String, Object>> getMonthlyRevenue(String role, String email, int year, int month);

    Map<String, Double> getMonthlyFeeSummary(String role, String email, int year, int month);

    Map<String, Double> getRevenueByPaymentModeForYear(String role, String email, int year);

    Map<String, Double> getRevenueByCourseForYear(String role, String email, int year);

    FeeFilterSummaryDTO getAdvancedFeeSummary(
            String role, String email, String timeFrame, LocalDate customStartDate, LocalDate customEndDate,
            String academicYear, String medium, String course, String feesStatus, String collectionType);
}
