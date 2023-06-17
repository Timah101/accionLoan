/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IndicinaRequestPayload {

    private String id;
    private String amount;
    private String narration;
    private String type;
    private String date;
    private String balance;
    private String client_id;
    private String client_secret;
    private String code;
    private CustomerPayload customer;
    private String account_id;
    private String start_date;
    private String end_date;
    private String request_type;
    private String accountNumber;
    private String status;
    private String message;
    private BankStatement bankStatement;
}
