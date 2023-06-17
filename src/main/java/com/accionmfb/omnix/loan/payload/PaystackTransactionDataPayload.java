package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackTransactionDataPayload {

    private String id;
    private String status;
    private String channel;
    private String reference;
    private PaystackAuthorizationPayload authorization;
    private PaystackTransactionCustomerPayload customer;
}
