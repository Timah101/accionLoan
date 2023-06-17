package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackChargeCardResponsePayload {
    private String status;
    private String message;
    private PaystackChargeCardDataPayload data;
    private String responseCode;
    private String responseMessage;
}
