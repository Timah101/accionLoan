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
public class CashFlowAnalysis {

    private String averageBalance;
    private String averageInflow;
    private String closingBalance;
    private String firstDay;
    private String lastDay;
    private String monthPeriod;
    private String noOfMonth;
    private String totalCreditTurnover;
    private String totalDebitTurnover;
    private String yearInStatement;
    private String accountActivity;
    private String averageCredits;
    private String averageDebits;
    private String netAverageMonthlyEarnings;
    private String noOfTransactingMonths;
}
