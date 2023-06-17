/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.repository;

import com.accionmfb.omnix.loan.model.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author bokon
 */
@Repository
@Transactional
public class LoanRepositoryImpl implements LoanRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    public UserActivity createUserActivity(UserActivity userActivity) {
        em.persist(userActivity);
        em.flush();
        return userActivity;
    }

    @Override
    public List<LoanSetup> getLoanTypes() {
        TypedQuery<LoanSetup> query = em.createQuery("SELECT t FROM LoanSetup t WHERE  t.status = 'ENABLED'", LoanSetup.class);
        List<LoanSetup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public AppUser getAppUserUsingUsername(String username) {
        TypedQuery<AppUser> query = em.createQuery("SELECT t FROM AppUser t WHERE t.username = :username", AppUser.class)
                .setParameter("username", username);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Customer getCustomerUsingMobileNumber(String mobileNumber) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.mobileNumber = :mobileNumber", Customer.class)
                .setParameter("mobileNumber", mobileNumber);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Account getCustomerAccount(Customer customer, String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.customer = :customer AND t.accountNumber = :accountNumber OR t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("customer", customer)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public LoanSetup getLoanTypeUsingCategory(String loanCategory) {
        TypedQuery<LoanSetup> query = em.createQuery("SELECT t FROM LoanSetup t WHERE t.loanCategory = :loanCategory", LoanSetup.class)
                .setParameter("loanCategory", loanCategory);
        List<LoanSetup> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Loan createLoan(Loan loan) {
        em.persist(loan);
        em.flush();
        return loan;
    }

    @Override
    public List<Loan> getLoanUsingCustomer(Customer customer) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.customer = :customer", Loan.class)
                .setParameter("customer", customer);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        else{
         return record;
        }
    }
    @Override
    public List<Loan> getActiveLoanUsingCustomer(Customer customer) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.customer = :customer and t.status <> 'DISBURSED'and t.status <> 'REJECTED'", Loan.class)
                .setParameter("customer", customer);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        else{
         return record;
        }
    }
      @Override
    public Loan getLoanUsingMobileNumber(String mobileNumber) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.mobileNumber = :mobileNumber", Loan.class)
                .setParameter("mobileNumber", mobileNumber);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
       return record.get(0);
    }

    @Override
    public Loan getPendingLoanUsingMobileNumber(String mobileNumber) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.mobileNumber = :mobileNumber AND t.status = 'PENDING'", Loan.class)
                .setParameter("mobileNumber", mobileNumber);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List <LoanOptions> getLoanOffersUsingLoanId(String loanId) {
        TypedQuery<LoanOptions> query = em.createQuery("SELECT t FROM LoanOptions t WHERE t.loanId = :loanId", LoanOptions.class)
                .setParameter("loanId", loanId);
        List<LoanOptions> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }
    
    @Override
    public SelectionScore createSelectionScore(SelectionScore selectionScore) {
        em.persist(selectionScore);
        em.flush();
        return selectionScore;
    }

    @Override
    public LoanSetup createLoanSetup(LoanSetup loanSetup) {
        em.persist(loanSetup);
        em.flush();
        return loanSetup;
    }

    @Override
    public Loan getLoanUsingLoanId(String loanId) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.loanId = :loanId", Loan.class)
                .setParameter("loanId", loanId);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Loan updateLoan(Loan loan) {
        em.merge(loan);
        em.flush();
        return loan;
    }

    @Override
    public List<Loan> getPendingArtisanLoan() {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.loanSetup.loanCategory = 'Artisan' AND t.status = 'Pending'", Loan.class);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<CreditBureauAddress> getCustomerAddress(Customer customer) {
        TypedQuery<CreditBureauAddress> query = em.createQuery("SELECT t FROM CreditBureauAddress t WHERE t.customer = :customer", CreditBureauAddress.class)
                .setParameter("customer", customer);
        List<CreditBureauAddress> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<CreditBureauCreditRisk> getCRCCreditRiskUsingCustomer(Customer customer) {
        TypedQuery<CreditBureauCreditRisk> query = em.createQuery("SELECT t FROM CreditBureauCreditRisk t WHERE t.customer = :customer", CreditBureauCreditRisk.class)
                .setParameter("customer", customer);
        List<CreditBureauCreditRisk> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<CreditBureauPerformanceSummary> getCreditBureauPerformanceSummaryUsingCustomer(Customer customer) {
        TypedQuery<CreditBureauPerformanceSummary> query = em.createQuery("SELECT t FROM CreditBureauPerformanceSummary t WHERE t.customer = :customer", CreditBureauPerformanceSummary.class)
                .setParameter("customer", customer);
        List<CreditBureauPerformanceSummary> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public BVN getBVN(String bvn) {
        TypedQuery<BVN> query = em.createQuery("SELECT t FROM BVN t WHERE t.customerBvn = :bvn", BVN.class)
                .setParameter("bvn", bvn);
        List<BVN> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public BVN getBVN(Customer customer, String mobileNumber) {
        TypedQuery<BVN> query = em.createQuery("SELECT t FROM BVN t WHERE t.customer = :customer AND t.mobileNumber = :mobileNumber", BVN.class)
                .setParameter("customer", customer)
                .setParameter("mobileNumber", mobileNumber);
        List<BVN> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

//    @Override
//    public BVN getBVN( String mobileNumber) {
//        TypedQuery<BVN> query = em.createQuery("SELECT t FROM BVN  t.mobileNumber = :mobileNumber", BVN.class)
//                .setParameter("customer", mobileNumber)
//                .setParameter("mobileNumber", mobileNumber);
//        List<BVN> record = query.getResultList();
//        if (record.isEmpty()) {
//            return null;
//        }
//        return record.get(0);
//    }
    @Override
    public Identification getIdentityUsingIdNumber(String idNumber) {
        TypedQuery<Identification> query = em.createQuery("SELECT t FROM Identification t WHERE t.idNumber = :idNumber", Identification.class)
                .setParameter("idNumber", idNumber);
        List<Identification> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Customer updateCustomer(Customer customer) {
        em.merge(customer);
        em.flush();
        return customer;
    }

    @Override
    public EconomicActivityLimit getEconomicActivityLimit(String economicActivity, String salesRank) {
        TypedQuery<EconomicActivityLimit> query = em.createQuery("SELECT t FROM EconomicActivityLimit t WHERE t.activity = :economicActivity AND t.salesRank = :salesRank", EconomicActivityLimit.class)
                .setParameter("economicActivity", economicActivity)
                .setParameter("salesRank", salesRank);
        List<EconomicActivityLimit> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Loan getRecordUsingRequestId(String requestId) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.requestId = :requestId", Loan.class)
                .setParameter("requestId", requestId);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public LoanOptions getLoanOptionsUsingId(String loanOptionId, String loanId) {
        TypedQuery<LoanOptions> query = em.createQuery("SELECT t FROM LoanOptions t WHERE t.loanOptionId = :loanOptionId AND t.loanId = :loanId", LoanOptions.class)
                .setParameter("loanOptionId", loanOptionId)
                .setParameter("loanId", loanId);
        List<LoanOptions> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public LoanOptions createLoanOptions(LoanOptions loanOption) {
        em.persist(loanOption);
        em.flush();
        return loanOption;
    }

    @Override
    public List<Loan> getActiveArtisanLoans() {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.status = 'DISBURSED' AND t.loanSetup.loanCategory = 'Artisan'", Loan.class);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public RubyxLoanRenewal getRubyxRenewalUsingRequestId(String requestId) {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.requestId = :requestId", RubyxLoanRenewal.class)
                .setParameter("requestId", requestId);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public RubyxLoanRenewal createRubyxLoanRenewal(RubyxLoanRenewal rubyxRenewal) {
        em.persist(rubyxRenewal);
        em.flush();
        return rubyxRenewal;
    }

    @Override
    public RubyxLoanRenewal getRubyxRenewalUsingDetails(String customerNumber, String renewalScore, String renewalRating) {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.customerNumber = :customerNumber AND t.renewalScore = :renewalScore AND t.renewalRating = :renewalRating", RubyxLoanRenewal.class)
                .setParameter("customerNumber", customerNumber)
                .setParameter("renewalScore", renewalScore)
                .setParameter("renewalRating", renewalRating);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<RubyxLoanRenewal> getRubyxLoanRenewalForCreditBureau() {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.creditBureauSearchDone = false AND t.status = 'SUCCESS'", RubyxLoanRenewal.class);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public RubyxLoanRenewal updateRubyxLoanRenewal(RubyxLoanRenewal rubyxLoanRenewal) {
        em.merge(rubyxLoanRenewal);
        em.flush();
        return rubyxLoanRenewal;
    }

    @Override
    public Customer getCustomerUsingCustomerNumber(String customerNumber) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.customerNumber = :customerNumber", Customer.class)
                .setParameter("customerNumber", customerNumber);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<RubyxLoanRenewal> getRubyxLoanRenewalForProfiling() {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.status = 'SUCCESS' AND t.customerSignOfferLetter = true AND t.guarantorSignOfferLetter = true AND t.disbursementAccount != ''", RubyxLoanRenewal.class);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Loan getPendingRubyxLoanRenewal(Customer customer, LoanSetup loanSetup) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE t.customer = :customer AND t.status = 'PENDING' AND t.loanSetup = :loanSetup", Loan.class)
                .setParameter("customer", customer)
                .setParameter("loanSetup", loanSetup);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public RubyxLoanRenewal getRubyxRenewalUsingLoanId(String loanId) {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.id = :loanId", RubyxLoanRenewal.class)
                .setParameter("loanId", Long.valueOf(loanId));
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Branch getBranchUsingBranchCode(String branchCode) {
        TypedQuery<Branch> query = em.createQuery("SELECT t FROM Branch t WHERE t.branchCode = :branchCode", Branch.class)
                .setParameter("branchCode", branchCode);
        List<Branch> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public RubyxLoanRenewal getRubyxRenewalUsingCustomerNumber(String customerNumber) {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.customerNumber = :customerNumber", RubyxLoanRenewal.class)
                .setParameter("customerNumber", customerNumber);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<RubyxLoanRenewal> getCompletedRubyxLoanRenewal() {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.customerApply = true "
                + "AND t.customerSignOfferLetter = true AND t.guarantorSignOfferLetter = true AND t.guarantorIdVerified = true "
                + "AND t.creditBureauSearchDone = true AND t.status = 'ACCEPTED' AND t.brightaCommitmentAccount != '' "
                + "AND t.loanAmount != '' AND t.currentLoanCycle != '' AND t.disbursementAccount != '' AND t.tenor != ''"
                + "AND t.interestRate != ''", RubyxLoanRenewal.class);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Account getAccountUsingAccountNumber(String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.accountNumber = :accountNumber OR t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Account> getCustomerAccounts(Customer customer) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.customer = :customer", Account.class)
                .setParameter("customer", customer);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<RubyxLoanRenewal> getRubyxLoanRenewalForPeriod(Date startDate, Date endDate) {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE CAST(t.createdAt AS date) >= :startDate AND CAST(t.createdAt AS date) <= :endDate ", RubyxLoanRenewal.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<RubyxLoanRenewal> getSignedOffRubyxLoanRenewal() {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.customerSignOfferLetter = true AND t.guarantorSignOfferLetter = true AND t.guarantorIdVerified = true AND t.status != 'ACCEPTED'", RubyxLoanRenewal.class);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Loan getLoanUsingRequestId(String requestId) {
        TypedQuery<Loan> query = em.createQuery("SELECT t FROM Loan t WHERE  t.requestId = :requestId", Loan.class)
                .setParameter("requestId", requestId);
        List<Loan> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }
    
     @Override
    public List<RubyxLoanRenewal> getBvn() {
        TypedQuery<RubyxLoanRenewal> query = em.createQuery("SELECT t FROM RubyxLoanRenewal t WHERE t.bvn is NULL AND t.failureReason = 'The customer has no BVN record set' AND t.status = 'FAILED'", RubyxLoanRenewal.class);
        List<RubyxLoanRenewal> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

     @Override
    public List<RubyxBacklogs> getRubyxBacklogs(String status) {
        TypedQuery<RubyxBacklogs> query = em.createQuery("SELECT t FROM RubyxBacklogs t where t.status = :status", RubyxBacklogs.class)
                .setParameter("status", status);
        List<RubyxBacklogs> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public RubyxBacklogs updateRubyxBacklogs(RubyxBacklogs oRubyxBacklogs) {
        em.merge(oRubyxBacklogs);
        em.flush();
        return oRubyxBacklogs;
    }
    
  
    @Override
    public Repayment createRepayment(Repayment oRepayment){
        em.persist(oRepayment);
        em.flush();
        return oRepayment;
    }

    @Override
    public Schedule createSchedule(Schedule schedule) {
        em.persist(schedule);
        em.flush();
        return schedule;
    }
    @Override
    public List<Schedule> getSchedule(String loanId){
        TypedQuery<Schedule> query = em.createQuery("SELECT t FROM Schedule t where t.loanId = :loanId", Schedule.class)
                .setParameter("loanId", loanId);
        List<Schedule> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Schedule> getScheduleByRepaymentDate(LocalDate currentDate){
        TypedQuery<Schedule> query = em.createQuery("SELECT t FROM Schedule t where t.repaymentDate >= :currentDate", Schedule.class)
                .setParameter("currentDate", currentDate);
        List<Schedule> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }
    @Override
    public List<Schedule> getScheduleByPastRepaymentDate(LocalDate currentDate){
        TypedQuery<Schedule> query = em.createQuery("SELECT t FROM Schedule t where t.repaymentDate <= :currentDate", Schedule.class)
                .setParameter("currentDate", currentDate);
        List<Schedule> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }
    @Override
    public List<Schedule> getScheduleByRepaymentDateAndLoanId(String loanId, LocalDate repaymentDate){
        TypedQuery<Schedule> query = em.createQuery("SELECT t FROM Schedule t where t.loanId = :loanId and t.repaymentDate >= :repaymentDate and t.status != 'Paid'", Schedule.class)
                .setParameter("loanId",loanId)
                .setParameter("repaymentDate", repaymentDate);
        List<Schedule> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }
    @Override
    public int deleteScheduleByDate(LocalDate repaymentDate){
        return em.createQuery("DELETE FROM Schedule t where t.repaymentDate = :repaymentDate")
                 .setParameter("repaymentDate", repaymentDate)
                .executeUpdate();
    }
    public Schedule getScheduleByMonthAndId(String months, String loanId){
        TypedQuery<Schedule> query = em.createQuery("SELECT t FROM Schedule t where t.months = :months and t.loanId = :loanId", Schedule.class)
                .setParameter("months", months)
                .setParameter("loanId", loanId);

        List <Schedule> record = query.getResultList();
        if(record.isEmpty()){
            return null;
        }
        return record.get(0);
    }

    @Override
    public Schedule updateSchedule(Schedule schedule) {
        em.merge(schedule);
        em.flush();
        return schedule;
    }

    @Override
    public List<Loan> getAllLoanByStatus() {
        TypedQuery<Loan> query = em.createQuery("SELECT t from Loan t where t.status = 'STAGE2'", Loan.class);

        List<Loan> record = query.getResultList();
        if(record.isEmpty()){
            return null;
        }
        return record;
    }

    @Override
    public PaystackDetails createPaystack(PaystackDetails paystackDetails) {
        em.persist(paystackDetails);
        em.flush();
        return paystackDetails;
    }

    @Override
    public PaystackDetails updatePaystack(PaystackDetails paystackDetails) {
        em.merge(paystackDetails);
        em.flush();

        return paystackDetails;
    }

    @Override
    public PaystackDetails getPaystackDetailsWithLoanId(String loanId) {
        TypedQuery<PaystackDetails> query = em.createQuery("Select t from PaystackDetails  t where t.loanId = :loanId", PaystackDetails.class)
                .setParameter("loanId", loanId);
        List<PaystackDetails> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public PaystackLoanRepaymentRecord createPaystackLoanRepaymentRecord(PaystackLoanRepaymentRecord createPaystackLoanRepaymentRecord) {
        em.persist(createPaystackLoanRepaymentRecord);
        em.flush();
        return createPaystackLoanRepaymentRecord;
    }

    @Override
    public PaystackLoanRepaymentRecord updatePaystackLoanRepaymentRecord(PaystackLoanRepaymentRecord createPaystackLoanRepaymentRecord) {
        em.merge(createPaystackLoanRepaymentRecord);
        em.flush();

        return createPaystackLoanRepaymentRecord;
    }

    @Override
    public PlaceLien createLienRecord(PlaceLien createLienRecord) {
        em.persist(createLienRecord);
        em.flush();
        return createLienRecord;
    }

    @Override
    public PlaceLien updateLienRecord(PlaceLien createLienRecord) {
        em.merge(createLienRecord);
        em.flush();
        return createLienRecord;
    }
}
