package com.newadmission.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeeFilterSummaryDTO {
    private Double totalFees;
    private Double paidFees;
    private Double pendingFees;

    // Helper method to merge Installment data with Direct Admission data
    public void add(FeeFilterSummaryDTO other) {
        if (other != null) {
            this.totalFees += other.getTotalFees();
            this.paidFees += other.getPaidFees();
            this.pendingFees += other.getPendingFees();
        }
    }
}