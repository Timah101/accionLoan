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
public class SpendAnalysis {

    private String airtime;
    private String bills;
    private String atmWithdrawalsSpend;
    private String averageRecurringExpense;
    private String bankCharges;
    private String cableTv;
    private String clubsAndBars;
    private String gambling;
    private String hasRecurringExpense;
    private String internationalTransactionsSpend;
    private String posSpend;
    private String religiousGiving;
    private String spendOnTransfers;
    private String totalExpenses;
    private String ussdTransactions;
    private String utilitiesAndInternet;
    private String webSpend;
}
