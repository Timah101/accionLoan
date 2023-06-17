package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackAuthorizationPayload {

    private String authorization_code;
    private String bin;
    private String last4;
    private String exp_month;
    private String exp_year;
    private String channel;
    private String card_type;
    private String bank;
    private String country_code;
    private String brand;
    private String reusable;
    private String signature;
    private String account_name;
}
