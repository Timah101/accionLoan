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
public class DisburseLoanRequestPayload {

    private String customerId;
    private String currency;
    private String amount;
    private String valueDate;
    private String maturityDate;
    private String category;
    private String drawDownAccount;
    private String interestRate;
    private String frequency;
    private String branchCode;
    private String targetAccountNumber;
    private boolean insuranceFee;
    private boolean adminFee;
    private String rubyxLoan;
}
