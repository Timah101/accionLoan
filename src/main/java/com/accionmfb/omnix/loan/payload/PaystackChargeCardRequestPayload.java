package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackChargeCardRequestPayload {
    private String amount;
    private String email;
    private String authorization_code;
    private String loanId;
}
