package com.newadmission.DTO;

import lombok.Data;

@Data
public class RazorpayVerifyRequest
{
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String branchCode;
    private String systemName;
}
