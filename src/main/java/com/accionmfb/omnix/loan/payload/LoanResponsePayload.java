/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import com.accionmfb.omnix.loan.model.RubyxLoanRenewal;
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
public class LoanResponsePayload {

    private String responseCode;
    private String loanId;
    private String loanAmountRequested;
    private String loanAmountApproved;
    private String loanTenor;
    private String interestRate;
    private String monthlyRepayment;
    private String firstRepaymentDate;
    private String customerName;
    private String loanType;
    private String loanDisbursementId;
    private String status;
    private String loanScore;
    private String loanRating;
    private String mobileNumber;
    private String totalLoans;
    private String totalPerformingLoans;
    private String totalLoanBalance;
    private String totalPerformingLoanBalance;
    private String customerSigned;
    private String guarantorSigned;
    private String guarantorIdVerified;
    private String customerApplied;
    private List<LoanOfferResponsePayload> loanOffers;
    private String brightaCommitmentAccount;
    private String customerNumber;
    private String disbursementAccount;
    private String bvn;
    private String accountOfficerCode;
    private String productCode;
    private String creditBureauSearchDone;
    private String loanCycle;
    private List<RubyxLoanRenewal> rubyxLoanData;
    private String createdAt;
    private String branch;
    private String renewalScore;
    private String renewalRating;
    private String renewalAmount;
    private String loanAmount;
    private String tenor;
    private String failureReason;
    private boolean customerSmsSent;
    private boolean contactCenterEmailSent;
    private boolean customerSignOfferLetter;
    private boolean guarantorSignOfferLetter;
    private String totalPerformingBalance;
    private String noOfPerformingLoans;
    private int noOfTotalLoans;
    private boolean creditBureauNoHit;
    private String currentLoanCycle;
    private String responseDescription;
    private boolean customerApply;
    private String residenceAddress;
    private String residenceCity;
    private String residenceState;
}
