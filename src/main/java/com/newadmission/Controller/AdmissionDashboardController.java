package com.newadmission.Controller;

import com.newadmission.Service.AdmissionFeeDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "https://pjsofttech.in")
@RequestMapping("/api/dashboard")
public class AdmissionDashboardController {

    @Autowired
    private AdmissionFeeDashboardService admissionService;

    @GetMapping("/revenue-by-month")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenueGraph(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam int year,
            @RequestParam int month) {

        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(admissionService.getMonthlyRevenue(role, email, year, month));
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<Map<String, Double>> getMonthlySummary(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam int year,
            @RequestParam int month) {

        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(admissionService.getMonthlyFeeSummary(role, email, year, month));
    }

    @GetMapping("/revenue-by-payment-mode")
    public ResponseEntity<Map<String, Double>> getPaymentModeGraph(
            @RequestParam String role,
            @RequestParam String email, @RequestParam int year) {
        // Basic validation
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(admissionService.getRevenueByPaymentModeForYear(role, email, year));
    }

    @GetMapping("/revenue-by-course")
    public ResponseEntity<Map<String, Double>> getCourseRevenueGraph(
            @RequestParam String role, @RequestParam String email,
            @RequestParam int year) {
        // Basic validation
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(admissionService.getRevenueByCourseForYear(role, email, year));
    }

    @GetMapping("/revenue-year-range")
    public ResponseEntity<List<Map<String, Object>>> getYearRangeRevenueGraph(
            @RequestParam int startYear,
            @RequestParam int endYear,
            @RequestParam String role,
            @RequestParam String email) {

        // Basic validation
        if (startYear > endYear || startYear < 2000) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(admissionService.getRevenueByYearRange(startYear, endYear, role, email));
    }

    @GetMapping("/revenue-month-wise-by-year")
    public ResponseEntity<List<Map<String, Object>>> getMonthWiseRevenueGraph(
            @RequestParam int year,
            @RequestParam String role,
            @RequestParam String email) {

        // Basic validation
        if (year < 2000 || year > 2100) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(admissionService.getMonthlyRevenueForSpecificYear(year, role, email));
    }
}