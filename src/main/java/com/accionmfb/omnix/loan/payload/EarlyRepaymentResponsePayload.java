package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class EarlyRepaymentResponsePayload {

    private String amount;
    private String responseCode;
    private String responseMessage;
}
