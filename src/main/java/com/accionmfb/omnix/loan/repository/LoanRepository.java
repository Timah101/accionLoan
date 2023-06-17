/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.repository;

import com.accionmfb.omnix.loan.model.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 *
 * @author bokon
 */
public interface LoanRepository {

    List<LoanSetup> getLoanTypes();

    UserActivity createUserActivity(UserActivity userActivity);

    AppUser getAppUserUsingUsername(String username);

    Customer getCustomerUsingMobileNumber(String mobileNumber);

    Customer getCustomerUsingCustomerNumber(String customerNumber);

    Account getCustomerAccount(Customer customer, String accountNumber);

    LoanSetup getLoanTypeUsingCategory(String category);

    Loan createLoan(Loan loan);

    Loan getLoanUsingLoanId(String loanId);

    List<Loan> getLoanUsingCustomer(Customer customer);

    SelectionScore createSelectionScore(SelectionScore selectionScore);

    LoanSetup createLoanSetup(LoanSetup loanSetup);

    Loan updateLoan(Loan loan);


    List<Loan> getPendingArtisanLoan();

    List<CreditBureauAddress> getCustomerAddress(Customer customer);

    List<CreditBureauCreditRisk> getCRCCreditRiskUsingCustomer(Customer customer);

    List<CreditBureauPerformanceSummary> getCreditBureauPerformanceSummaryUsingCustomer(Customer customer);

    Identification getIdentityUsingIdNumber(String idNumber);

    BVN getBVN(String bvn);

    BVN getBVN(Customer customer, String mobileNumber);

    Customer updateCustomer(Customer customer);

    EconomicActivityLimit getEconomicActivityLimit(String economicActivity, String salesRank);

    Loan getRecordUsingRequestId(String requestId);

    LoanOptions createLoanOptions(LoanOptions loanOption);

    LoanOptions getLoanOptionsUsingId(String loanOptionId, String loanId);

    List<Loan> getActiveArtisanLoans();

    RubyxLoanRenewal getRubyxRenewalUsingRequestId(String requestId);

    RubyxLoanRenewal createRubyxLoanRenewal(RubyxLoanRenewal rubyxRenewal);

    RubyxLoanRenewal getRubyxRenewalUsingDetails(String customerNumber, String renewalScore, String renewalRating);

    List<RubyxLoanRenewal> getRubyxLoanRenewalForCreditBureau();

    List<RubyxLoanRenewal> getRubyxLoanRenewalForProfiling();

    List<RubyxLoanRenewal> getCompletedRubyxLoanRenewal();

    RubyxLoanRenewal updateRubyxLoanRenewal(RubyxLoanRenewal rubyxLoanRenewal);

    RubyxLoanRenewal getRubyxRenewalUsingCustomerNumber(String customerNumber);

    RubyxLoanRenewal getRubyxRenewalUsingLoanId(String loanId);

    Loan getPendingRubyxLoanRenewal(Customer customer, LoanSetup loanType);

    Branch getBranchUsingBranchCode(String branchCode);

    Account getAccountUsingAccountNumber(String accountNumber);

    List<Account> getCustomerAccounts(Customer customer);

    List<RubyxLoanRenewal> getRubyxLoanRenewalForPeriod(Date startDate, Date endDate);

    List<RubyxLoanRenewal> getSignedOffRubyxLoanRenewal();

    Loan getLoanUsingRequestId(String requestId);
    
    List<RubyxLoanRenewal> getBvn();
    
    public Loan getLoanUsingMobileNumber(String mobileNumber);

    public Loan getPendingLoanUsingMobileNumber(String mobileNumber);

    public List <LoanOptions> getLoanOffersUsingLoanId(String loanId);

    public List<RubyxBacklogs> getRubyxBacklogs(String status);

    public RubyxBacklogs updateRubyxBacklogs(RubyxBacklogs oRubyxBacklogs);

    public List<Loan> getActiveLoanUsingCustomer(Customer customer);

    Schedule createSchedule(Schedule schedule);
    Repayment createRepayment(Repayment oRepayment);

    public List<Schedule> getSchedule(String loanId);

    public List<Schedule> getScheduleByRepaymentDate(LocalDate repaymentDate);



    public List<Schedule> getScheduleByRepaymentDateAndLoanId(String loanId, LocalDate repaymentDate);



    public int deleteScheduleByDate(LocalDate repaymentDate);

    public Schedule getScheduleByMonthAndId(String months, String loanId);

    Schedule updateSchedule(Schedule schedule);


    List<Loan> getAllLoanByStatus();

    PaystackDetails createPaystack (PaystackDetails paystackDetails);

    PaystackDetails updatePaystack(PaystackDetails paystackDetails);

    PaystackDetails getPaystackDetailsWithLoanId(String loanId);

    PaystackLoanRepaymentRecord createPaystackLoanRepaymentRecord (PaystackLoanRepaymentRecord paystackLoanRepaymentRecord);

    PaystackLoanRepaymentRecord updatePaystackLoanRepaymentRecord (PaystackLoanRepaymentRecord paystackLoanRepaymentRecord);

    PlaceLien createLienRecord(PlaceLien placeLien);

    PlaceLien updateLienRecord(PlaceLien placeLien);


//    public Loan getLoanOffer(Customer customer);
}
