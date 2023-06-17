package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PlaceLienResponsePayload {

    private String lockId;
    private String loanId;
    private String amount;
    private String status;
    private String responseCode;
    private String responseMessage;
}
