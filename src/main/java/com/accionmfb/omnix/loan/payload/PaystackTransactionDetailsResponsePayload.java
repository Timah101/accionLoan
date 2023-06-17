package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackTransactionDetailsResponsePayload {
    private String status;
    private String message;
    private PaystackTransactionDataPayload data;
    private String responseCode;
    private String responseMessage;
}
