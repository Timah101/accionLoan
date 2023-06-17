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
public class LoanSetupResponsePayload {

    private String loanName;
    private String loanDescription;
    private String loanCategory;
    private String loanTenor;
    private String loanAmount;
    private String interestRate;
    private String adminFee;
    private String insuranceFee;
    private String processingFee;
    private String responseCode;
}
