package com.newadmission.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryWithCountsDTO {
    private long total;
    private long todayCount;
    private long last7DaysCount;
    private long last30DaysCount;
    private long last365DaysCount;
    private List<AttendanceSummary> data;
}