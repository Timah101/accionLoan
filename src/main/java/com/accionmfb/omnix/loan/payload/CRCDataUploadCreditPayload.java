/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

/**
 *
 * @author bokon
 */
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CRCDataUploadCreditPayload {

    private String CustomerID;
    private String AccountNumber;
    private String AccountStatus;
    private String AccountStatusDate;
    private String DateOfLoan;
    private String CreditLimit;
    private String LoanAmount;
    private String OutstandingBalance;
    private String InstalmentAmount;
    private String Currency;
    private String DaysInArrears;
    private String OverdueAmount;
    private String LoanType;
    private String LoanTenor;
    private String RepaymentFrequency;
    private String LastPaymentDate;
    private String LastPaymentAmount;
    private String MaturityDate;
    private String LoanClassification;
    private String LegalChallengeStatus;
    private String LitigationDate;
    private String ConsentStatus;
    private String LoanSecurityStatus;
    private String CollateralType;
    private String CollateralDetails;
    private String PreviousAccountNumber;
    private String PreviousName;
    private String PreviousCustomerID;
    private String PreviousBranchCode;
}
