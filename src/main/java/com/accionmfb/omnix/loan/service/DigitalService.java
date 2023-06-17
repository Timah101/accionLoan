/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.model.SMS;
import com.accionmfb.omnix.loan.payload.*;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.text.ParseException;

/**
 *
 * @author bokon
 */
public interface DigitalService {

    boolean validateDigitalLoanBookingPayload(String token, DigitalLoanRequestPayload requestPayload);

    String processDigitalLoanBooking(String token, DigitalLoanRequestPayload requestPayload);

    String processPendingLoanWithMobileNumber(String token, DigitalActiveLoanRequestPayload requestPayload);

    boolean validateDigitalLoanAcceptancePayload(String token, DigitalLoanAcceptanceRequestPayload requestPayload);

    String processDigitalLoanAcceptance(String token, DigitalLoanAcceptanceRequestPayload requestPayload);

    boolean validateDigitalLoanDeclinePayload (String token, DigitalLoanDeclineRequestPayload requestPayload);
    
   String processDigitalLoanDecline (String token, DigitalLoanDeclineRequestPayload requestPayload);
   
    boolean validateDigitalLoanDisbursementPayload(String token, LoanIdRequestPayload requestPayload);

    String processDigitalLoanDisbursement(String token, LoanIdRequestPayload requestPayload);

    boolean validateDigitalLoanRenewalPayload(String token, LoanRenewalRequestPayload requestPayload);

    String processDigitalLoanRenewal(String token, LoanRenewalRequestPayload requestPayload);
    
    boolean validateGuarantorUpdatePayload(String token, GuarantorUpdatePayload requestPayload);
    
    String processGuarantorUpdate(String token, GuarantorUpdatePayload requestPayload);
    
    boolean validateLoanApplicantDetailsPayload (String token, LoanApplicantDetailsPayload requestPayload);
    
    String processLoanApplicantDetails(String token, LoanApplicantDetailsPayload requestPayload);

    boolean validateDigitalLoanHistoryPayload(String token, DigitalLoanHistoryRequestPayload requestPayload);

    String processDigitalActiveLoan(String token, DigitalActiveLoanRequestPayload requestPayload);
    
    boolean validateDigitalActiveLoanPayload (String token, DigitalActiveLoanRequestPayload requestPayload);
    
    String processDigitalLoanHistory(String token, DigitalLoanHistoryRequestPayload requestPayload);

     LoanBalanceResponsePayload getLoanBalance(String t24Account, String token);

    ScheduleResponsePayload processSchedule(String token, String mobileNumber, String loanOptionsId);

    PaystackTransactionDetailsResponsePayload processPaystackCardTransactionDetails(String paystackRef, String loanId) throws UnirestException;

    PaystackChargeCardResponsePayload processPaystackCardForLoanRepayment(String token, PaystackChargeCardRequestPayload paystackChargeCardRequestPayload) throws UnirestException;

    String processEarlyRepayment(String token, EarlyRepaymentRequestPayload requestPayload) throws UnirestException, ParseException;

    SMSResponsePayload sendSms(SMS sms);

    Double flatRateLoanApplication(String approvedAmount, double interestRate, int tenor);

    PaystackLoanRepaymentResponse processPaystackCollectionFromPoolAccountToCustomerAccount(String token, String loanId, String amount);

    PlaceLienResponsePayload processPlaceLien(String token, LockAmountDto request) throws ParseException;

    String processCustomerLoanDetails(String token, DigitalLoanHistoryRequestPayload requestPayload);

    String processLiquidateLoanTemp(String token, String loanId) throws ParseException;
}
