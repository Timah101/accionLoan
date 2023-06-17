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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoanOfferResponsePayload {

    private String approvedAmount;
    private String tenor;
    private String rate;
    private String cummulativeRate;
    private String monthlyRepayment;
    private String totalRepayment;
    private String optionId;
    private ArrayList <String> repayments;
}
