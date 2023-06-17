package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackCardDetails {
    private String last4Digit;
    private boolean cardReusable;
    private String cardType;
    private boolean paystackCardAvailable;
}
