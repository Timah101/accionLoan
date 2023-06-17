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
public class Data {

    private String token;
    private BehaviouralAnalysis behaviouralAnalysis;
    private CashFlowAnalysis cashFlowAnalysis;
    private IncomeAnalysis incomeAnalysis;
    private SpendAnalysis spendAnalysis;
    private TransactionPatternAnalysis transactionPatternAnalysis;
    
    //Used for Mono account statement
    private String _id;
    private double amount;
    private String date;
    private String narration;
    private String type;
    private String category;
}
