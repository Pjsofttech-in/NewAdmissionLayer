//package com.newadmission.Entity;
//
//import com.newadmission.Service.InstallmentService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//public class DatabaseCleanupScheduler {
//
//    @Autowired
//    InstallmentService installmentService;
//
//    // ⚡ Configuration option A: Runs exactly every 5 seconds (5000 milliseconds)
////    @Scheduled(fixedRate = 5000)
////    public void clearExpiredSessions() {
////        System.out.println("Fixed Rate Task Executing at: " + LocalDateTime.now());
////    }
//
//    // ⏱️ Configuration option B: Runs daily at 12:00 AM (Midnight) using a Cron Expression
//    @Scheduled(cron = "0 */1 * * * ?")
//    public void generateDailyFeeReports() {
//        System.out.println("Midnight Cron Job Triggered!");
//        installmentService.getFeesDueInDays(7);
//    }
//}
