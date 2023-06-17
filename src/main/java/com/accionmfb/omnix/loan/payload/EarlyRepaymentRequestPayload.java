package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class EarlyRepaymentRequestPayload {

    private String loanId;
    private String amount;
    private String requestId;
    private String paymentMethod;
}
