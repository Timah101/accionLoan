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
public class IncomeAnalysis {

    private String averageOtherIncome;
    private String expectedSalaryDay;
    private String averageSalary;
    private String confidenceIntervalonSalaryDetection;
    private String lastSalaryDate;
    private String medianIncome;
    private String numberOtherIncomePayments;
    private String numberSalaryPayments;
    private String salaryEarner;
    private String salaryFrequency;
}
