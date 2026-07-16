package com.newadmission.DTO;

public interface MonthlyRevenueDTO {
    Integer getMonth(); // Will return 1 for Jan, 2 for Feb, etc.
    Double getTotalAmount();
}