package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class PaystackTransactionCustomerPayload {

    private Long id;
    private String first_name;
    private String last_name;
    private String email;
    private String customer_code;
    private String phone;

}
