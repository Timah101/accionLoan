package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackChargeCardDataPayload {

    private int amount;
    private String currency;
    private String transaction_date;
    private String status;
    private String reference;
    private String domain;
    private String metadata;
    private String gateway_response;
    private String message;
    private String channel;
    private String ip_address;
    private String log;
    private int fees;
}
