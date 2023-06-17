/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.model.Customer;
import com.accionmfb.omnix.loan.model.Loan;
import com.accionmfb.omnix.loan.payload.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author bokon
 */
public interface GenericService {

    void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId);

    void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status);

    String postToT24(String requestBody);

    String postToMiddleware(String requestEndpoint, String requestBody);

    String getT24TransIdFromResponse(String response);

    String validateT24Response(String responseString);

    String decryptString(String textToDecrypt, String encryptionKey);

    String getTextFromOFSResponse(String ofsResponse, String textToExtract);

    String formatDateWithHyphen(String dateToFormat);

    char getTimePeriod();

    String getPostingDate();

    String encryptString(String textToEncrypt, String token);

    String generateRequestId();

    String hashIdentityValidationRequest(IdentityRequestPayload requestPayload);

    String hashBVNValidationRequest(BVNRequestPayload requestPayload);

    String hashCreditBureauValidationRequest(CreditBureauRequestPayload requestPayload);

    String hashSMSNotificationRequest(SMSRequestPayload requestPayload);

    String hashEmailNotificationRequest(EmailRequestPayload requestPayload);

    String loanRepayment(BigDecimal principal, BigDecimal interestRate, int tenor);

    ArrayList<String> loanRepaymentFlatRateList(BigDecimal approvedAmount, BigDecimal interestRate, int tenor);

    String loanRepaymentFlatRate(BigDecimal principal, BigDecimal interestRate, int tenor);
    
      String loanRepaymentWithReducingBalance(BigDecimal principal, BigDecimal interestRate, int tenor);

    Object getLoanBalances(String customerBranch, String customerNo, String userCredentials);

    String checkAdminIP(String remoteAddress);

    String formatAmountWithComma(String amaountToFormat);

    String generateTransRef(String transType);

    List<LoanRepaymentSchedule> generateLoanRepaymentSchedule(Loan loan);

    boolean addressMatch(Customer customer);

    int getInquiryInLastThreeMonths(Customer customer);

    int getDishonouredCheque(Customer customer);

    int getMaxOverdueDays(Customer customer);

    int getSuitAndWriteOff(Customer customer);

    BigDecimal getDeliquentLoan(Customer customer);

    BigDecimal getMaxOverdueAmount(Customer customer);

    int getMaxOverdueFacility(Customer customer);

    boolean namesMatching(String[] namesToMatch, String[] namesToCompare);

    void createPdf(String dest, String text) throws IOException;

    CompletableFuture<String> sendDebitSMS(NotificationPayload requestPayload);

    CompletableFuture<String> sendCreditSMS(NotificationPayload requestPayload);

    CompletableFuture<String> sendDigitalLoanOfferEmail(NotificationPayload requestPayload);

    CompletableFuture<String> sendLoanSMS(NotificationPayload requestPayload);

    CompletableFuture<String> sendDigitalLoanAcceptanceEmail(NotificationPayload requestPayload);

    CompletableFuture<String> sendDigitalLoanDeclineEmail(NotificationPayload requestPayload);

    CompletableFuture<String> sendArtisanLoanOfferEmail(NotificationPayload requestPayload);

    CompletableFuture<String> uploadToCreditBureau(String token, Loan loan);

    CompletableFuture<String> sendRubyxLoanRenewalSMS(NotificationPayload requestPayload);

    CompletableFuture<String> sendRubyxLoanRenewalEmail(NotificationPayload requestPayload);

    String formatOfsUserCredentials(String ofs, String userCredentials);

    String hashAccountBalanceRequest(AccountNumberPayload requestPayload);

    String hashRubyxLoanRenewal(RubyxLoanRenewalPayload requestPayload, String token);

    String hashCustomerDetailsRequest(OmniRequestPayload requestPayload);

    String getActiveLoanFromMiddleware(String requestEndPoint, String requestBody);

    public List<String> getOfsValues(String ofsResponse, String fieldName);

    public String getOfsValue(String ofsResponse, String fieldName);

    String sendLoanEmail(NotificationPayload notificationPayload);

    public String postToSMSService(String requestEndPoint, String requestBody);

    String hashLocalTransferValidationRequest(LocalTransferWithInternalPayload requestPayload);

}
