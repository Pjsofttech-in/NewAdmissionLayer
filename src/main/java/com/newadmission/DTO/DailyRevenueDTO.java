package com.newadmission.DTO;

import java.time.LocalDate;

public interface DailyRevenueDTO {
    LocalDate getDate();
    Double getTotal();
}