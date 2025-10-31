package com.newadmission.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AttendanceSummary {
    String getStudentName();
    LocalDate getDate();
    LocalTime getLoginTime();     // from "LOGIN" record
    LocalTime getLogoutTime();    // from "LOGOUT" record
    String getLoginStatus();      // from "LOGIN" record
    Long getAdmissionId();
}