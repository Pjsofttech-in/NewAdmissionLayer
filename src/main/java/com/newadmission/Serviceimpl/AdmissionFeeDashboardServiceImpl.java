package com.newadmission.Serviceimpl;

import com.newadmission.DTO.*;
import com.newadmission.Repository.AdmissionRepository;
import com.newadmission.Repository.InstallmentRepository;
import com.newadmission.Service.AdmissionFeeDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdmissionFeeDashboardServiceImpl implements AdmissionFeeDashboardService {

    @Autowired
    InstallmentRepository installmentRepository;

    @Autowired
    AdmissionRepository admissionRepository;

    @Autowired
    StaffService staffService;

    @Override
    public List<Map<String, Object>> getMonthlyRevenueForSpecificYear(int year, String role, String email) {

        // 1. Security & Branch Check
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You don't have permission to view revenue data");
        }
        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        // 2. Fetch data from DB
        List<MonthlyRevenueDTO> installmentData =
                installmentRepository.getInstallmentRevenueByYearMonthWise(year, branchCode);
        List<MonthlyRevenueDTO> directAdmissionData =
                admissionRepository.getDirectAdmissionRevenueByYearMonthWise(year, branchCode);

        // 3. Create a map to quickly look up revenue by month number (1-12)
        Map<Integer, Double> monthlyTotals = new LinkedHashMap<>();

        for (MonthlyRevenueDTO dto : installmentData) {
            monthlyTotals.put(dto.getMonth(), dto.getTotalAmount());
        }

        for (MonthlyRevenueDTO dto : directAdmissionData) {
            monthlyTotals.put(
                    dto.getMonth(),
                    monthlyTotals.getOrDefault(dto.getMonth(), 0.0) + dto.getTotalAmount()
            );
        }

        // 4. Build the final 12-month array for the frontend
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            Map<String, Object> monthData = new LinkedHashMap<>();

            // Convert month number (1) to Name ("January") for cleaner UI
            String monthName = Month.of(i).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            monthData.put("month", monthName);
            monthData.put("revenue", monthlyTotals.getOrDefault(i, 0.0));

            chartData.add(monthData);
        }

        return chartData;
    }

    @Override
    public List<Map<String, Object>> getRevenueByYearRange(int startYear, int endYear, String role, String email) {

        // 1. Permission Check & Branch Fetching
        if (!staffService.hasPermission(role, email, "GET")) { // Adjust permission name as needed
            throw new AccessDeniedException("You don't have permission to view revenue data");
        }
        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        // 2. Define Date Range (Jan 1st of startYear to Dec 31st of endYear)
        LocalDate startDate = LocalDate.of(startYear, 1, 1);
        LocalDate endDate = LocalDate.of(endYear, 12, 31);

        // 3. Fetch from DB
        List<YearlyRevenueDTO> installmentData =
                installmentRepository.getInstallmentRevenueByYearRange(startDate, endDate, branchCode);
        List<YearlyRevenueDTO> directAdmissionData =
                admissionRepository.getDirectAdmissionRevenueByYearRange(startDate, endDate, branchCode);

        // 4. Quick lookup maps
        Map<Integer, Double> revenueMap = new LinkedHashMap<>();

        // Merge Installments
        for (YearlyRevenueDTO dto : installmentData) {
            revenueMap.put(dto.getYear(), dto.getTotalAmount());
        }

        // Merge Direct Admissions
        for (YearlyRevenueDTO dto : directAdmissionData) {
            revenueMap.put(dto.getYear(), revenueMap.getOrDefault(dto.getYear(), 0.0) + dto.getTotalAmount());
        }

        // 5. Build final list (filling in missing years with 0.0)
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (int currentYear = startYear; currentYear <= endYear; currentYear++) {
            Map<String, Object> yearlyPoint = new LinkedHashMap<>();
            yearlyPoint.put("year", currentYear);
            yearlyPoint.put("revenue", revenueMap.getOrDefault(currentYear, 0.0));
            chartData.add(yearlyPoint);
        }

        return chartData;
    }

    @Override
    public List<Map<String, Object>> getMonthlyRevenue(String role, String email, int year, int month) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You don't have permission to get fees data");
        }
        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 1. Fetch from BOTH sources
        List<DailyRevenueDTO> installmentData = installmentRepository.getDailyRevenueBetween(startDate, endDate, branchCode);
        List<DailyRevenueDTO> directAdmissionData = admissionRepository.getDirectAdmissionRevenueBetween(startDate, endDate, branchCode);

        // 2. Convert both to quick lookup maps
        Map<LocalDate, Double> installmentMap = installmentData.stream()
                .collect(Collectors.toMap(DailyRevenueDTO::getDate, DailyRevenueDTO::getTotal));

        Map<LocalDate, Double> admissionMap = directAdmissionData.stream()
                .collect(Collectors.toMap(DailyRevenueDTO::getDate, DailyRevenueDTO::getTotal));

        // 3. Build the final merged continuous list
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            // Safely get amounts, defaulting to 0.0 if nothing was found for that day
            double installmentTotal = installmentMap.getOrDefault(date, 0.0);
            double directTotal = admissionMap.getOrDefault(date, 0.0);

            // Merge them!
            double combinedDailyRevenue = installmentTotal + directTotal;

            Map<String, Object> dailyPoint = new LinkedHashMap<>();
            dailyPoint.put("date", date.toString());
            dailyPoint.put("revenue", combinedDailyRevenue);

            chartData.add(dailyPoint);
        }

        return chartData;
    }

    @Override
    public Map<String, Double> getMonthlyFeeSummary(String role, String email, int year, int month) {

        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You don't have permission to get fees data");
        }
        String branchCode = staffService.fetchBranchCodeByRole(role, email);
        // 1. Calculate the first and last day of the selected month
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 2. Fetch the data from both tables
        FeeSummaryProjection installmentSummary = installmentRepository.getInstallmentSummaryByMonth(startDate, endDate, branchCode);
        FeeSummaryProjection admissionSummary = admissionRepository.getDirectAdmissionSummaryByMonth(startDate, endDate, branchCode);

        // 3. Merge the results
        double totalFees = installmentSummary.getTotalFees() + admissionSummary.getTotalFees();
        double paidFees = installmentSummary.getPaidFees() + admissionSummary.getPaidFees();
        double pendingFees = installmentSummary.getPendingFees() + admissionSummary.getPendingFees();

        // 4. Return as a clean Map for JSON serialization
        Map<String, Double> response = new HashMap<>();
        response.put("totalFees", totalFees);
        response.put("paidFees", paidFees);
        response.put("pendingFees", pendingFees);

        return response;
    }

    @Override
    public Map<String, Double> getRevenueByPaymentModeForYear(
            String role, String email, int year) {
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You don't have permission to get fees data");
        }
        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 1. Fetch from both sources
        List<PaymentModeSummaryDTO> installmentData =
                installmentRepository.getInstallmentRevenueByPaymentMode(startDate, endDate, branchCode);
        List<PaymentModeSummaryDTO> directAdmissionData =
                admissionRepository.getDirectAdmissionRevenueByPaymentMode(startDate, endDate, branchCode);

        // 2. Create the final map
        Map<String, Double> mergedModeSummary = new HashMap<>();

        // 3. Helper lambda to process and merge the lists cleanly
        mergeDataIntoMap(installmentData, mergedModeSummary);
        mergeDataIntoMap(directAdmissionData, mergedModeSummary);

        return mergedModeSummary;
    }

    // Helper method to keep code clean and handle null/empty strings
    private void mergeDataIntoMap(List<PaymentModeSummaryDTO> dataList, Map<String, Double> mergedMap) {
        for (PaymentModeSummaryDTO data : dataList) {

            // Clean up the string. If it's null or blank, default to "Unknown"
            String mode = (data.getPaymentMode() != null && !data.getPaymentMode().trim().isEmpty())
                    ? data.getPaymentMode().trim()
                    : "Unknown";

            // Add to existing total for this mode, or start at 0.0
            mergedMap.put(mode, mergedMap.getOrDefault(mode, 0.0) + data.getTotalAmount());
        }
    }

    @Override
    public Map<String, Double> getRevenueByCourseForYear(String role, String email, int year) {

        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("You don't have permission to get fees data");
        }
        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // 1. Fetch from both sources
        List<CourseRevenueDTO> installmentData =
                installmentRepository.getInstallmentRevenueByCourse(startDate, endDate, branchCode);
        List<CourseRevenueDTO> directAdmissionData =
                admissionRepository.getDirectAdmissionRevenueByCourse(startDate, endDate, branchCode);

        // 2. Create the final map
        Map<String, Double> mergedCourseSummary = new HashMap<>();

        // 3. Helper lambda to process and merge the lists
        mergeCourseDataIntoMap(installmentData, mergedCourseSummary);
        mergeCourseDataIntoMap(directAdmissionData, mergedCourseSummary);

        return mergedCourseSummary;
    }

    // Helper method to keep code clean and handle null/empty strings
    private void mergeCourseDataIntoMap(List<CourseRevenueDTO> dataList, Map<String, Double> mergedMap) {
        for (CourseRevenueDTO data : dataList) {

            // Clean up the string. If it's null or blank, default to "Uncategorized"
            String course = (data.getCourseName() != null && !data.getCourseName().trim().isEmpty())
                    ? data.getCourseName().trim()
                    : "Uncategorized";

            // Add to existing total for this course, or start at 0.0
            mergedMap.put(course, mergedMap.getOrDefault(course, 0.0) + data.getTotalAmount());
        }
    }

    @Override
    public FeeFilterSummaryDTO getAdvancedFeeSummary(
            String role, String email, String timeFrame, LocalDate customStartDate, LocalDate customEndDate,
            String academicYear, String medium, String course, String feesStatus, String collectionType) {

        // 1. Security Check
        if (!staffService.hasPermission(role, email, "GET")) {
            throw new AccessDeniedException("Unauthorized to view reports");
        }
        String branchCode = staffService.fetchBranchCodeByRole(role, email);

        // 2. Resolve the TimeFrame into Date Objects
        LocalDate startDate = null;
        LocalDate endDate = null;
        LocalDate today = LocalDate.now();

        if (timeFrame != null && !timeFrame.trim().isEmpty()) {
            switch (timeFrame.toUpperCase()) {
                case "TODAY":
                    startDate = today;
                    endDate = today;
                    break;
                case "LAST 7 DAYS":
                    startDate = today.minusDays(7);
                    endDate = today;
                    break;
                case "LAST 30 DAYS":
                    startDate = today.minusDays(30);
                    endDate = today;
                    break;
                case "LAST 365 DAYS":
                    startDate = today.minusDays(365);
                    endDate = today;
                    break;
                case "CUSTOM DATE RANGE":
                    startDate = customStartDate;
                    endDate = customEndDate;
                    break;
            }
        }

        // 3. Fetch from Admission (Direct Fees)
        FeeFilterSummaryDTO summary = admissionRepository.getDirectAdmissionFeeSummary(
                startDate, endDate, academicYear, medium, course, feesStatus, collectionType, branchCode
        );

        // 4. Fetch from Installments
        FeeFilterSummaryDTO installmentSummary = installmentRepository.getInstallmentFeeSummary(
                startDate, endDate, academicYear, medium, course, feesStatus, collectionType, branchCode
        );

        // 5. Merge the Results
        if (summary == null) {
            summary = new FeeFilterSummaryDTO(0.0, 0.0, 0.0);
        }
        summary.add(installmentSummary);

        return summary;
    }
}
