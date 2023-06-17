/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import java.util.List;
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
public class TransactionPatternAnalysis {

    private String lastDateOfCredit;
    private String lastDateOfDebit;
    private String mostFrequentBalanceRange;
    private String mostFrequentTransactionRange;
    private List<RecurringExpense> recurringExpense;
    private String transactionsBetween100000And500000;
    private String transactionsBetween10000And100000;
    private String transactionsGreater500000;
    private String transactionsLess10000;
    private MonthsAndWeeks MAWWZeroBalanceInAccount;
    private String NODWBalanceLess5000;
    private MonthsAndWeeks highestMAWOCredit;
    private MonthsAndWeeks highestMAWODebit;
}
