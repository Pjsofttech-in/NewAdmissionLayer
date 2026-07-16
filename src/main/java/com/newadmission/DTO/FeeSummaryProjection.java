package com.newadmission.DTO;

public interface FeeSummaryProjection {
    Double getTotalFees();
    Double getPaidFees();
    Double getPendingFees();
}
