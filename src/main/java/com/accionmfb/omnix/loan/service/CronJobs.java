/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.model.*;
import com.accionmfb.omnix.loan.payload.*;
import com.accionmfb.omnix.loan.repository.LoanRepository;
import com.accionmfb.omnix.loan.repository.SmsRepository;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author bokon
 */
@Service
@Slf4j
public class CronJobs {

    @Autowired
    AccountService accountService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    GenericService genericService;
    @Autowired
    ArtisanService artisanService;
    @Autowired
    RubyxLoanRenewalService rubyxService;
    @Autowired
    LoanRepository loanRepository;
    @Autowired
    Gson gson;
    @Autowired
    SmsRepository smsRepository;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    CreditBureauService creditBureauService;
    @Autowired
    DigitalService digitalService;

    @Value("${omnix.digital.branch.code}")
    private String digitalBranchCode;
    @Value("${omnix.version.customer}")
    private String customerVersion;
    private String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJydWJ5eCIsInJvbGVzIjoiW1JVQllYLCBDUkVESVRfQlVSRUFVX1ZBTElEQVRJT04sIEFDQ09VTlRfQkFMQU5DRSwgU01TX05PVElGSUNBVElPTiwgVFJBTlNBQ1RJT05fRU1BSUxfQUxFUlQsIExPQU5fT0ZGRVJfRU1BSUxfQUxFUlRdIiwiYXV0aCI6IlZSNmtONHdHamFDYVZtNzBpVkR1WEE9PSIsIkNoYW5uZWwiOiJNb2JpbGUiLCJJUCI6IjA6MDowOjA6MDowOjA6MSIsImlzcyI6IkFjY2lvbiBNaWNyb2ZpbmFuY2UgQmFuayIsImlhdCI6MTY1NTQ2ODI2MywiZXhwIjo2MjUxNDI4NDQwMH0.Pw0TKpXLbV1ny59TD2RXfclAHn_ZdYYDuyCNTQCZFCI";
    String bucketName = "accion-ng_download";
    String objectBaseName = "alerts/accion-ng_alerts_";
    String projectId = "accion-ng";

    @Scheduled(fixedDelay = 600000, initialDelay = 1000)
//    public void fetchRubyxLoanRenewalFromCloud() throws FileNotFoundException, IOException {
//        System.out.println("--->  fetchRubyxLoanRenewalFromCloud  <------");
//        try {
//            String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
//            String rubyxLoanRenewalFile = "C:\\Omnix\\DataExtraction\\Rubyx\\Loan" + formattedDate.replace("-", "") + ".json";
//            Path path = Paths.get(rubyxLoanRenewalFile, new String[0]);
//
//            // addded by Daniel Ofoleta to ensure skipped loans are reprocessed  on a later date
//            List<RubyxBacklogs> oBacklogList = loanRepository.getRubyxBacklogs("OPEN");
//            if (oBacklogList == null || oBacklogList.isEmpty()) {
//                oBacklogList = new ArrayList<>();
//            }
//
//            if (Files.notExists(path, new java.nio.file.LinkOption[0]) || !oBacklogList.isEmpty()) {
//
//                GoogleCredentials googleCredentials = GoogleCredentials.fromStream(new FileInputStream("C:\\Omnix\\accion-ng-5b86be14b180.json"));
//                Storage storage = (Storage) ((StorageOptions.Builder) ((StorageOptions.Builder) StorageOptions.newBuilder().setCredentials((Credentials) googleCredentials)).setProjectId(projectId)).build().getService();
//
//                // a loop is addded by Daniel Ofoleta to ensure skipped loans are reprocessed  on a later date
//                // include current date in the loop
//                if (Files.notExists(path, new java.nio.file.LinkOption[0])) {
//                    RubyxBacklogs oRubyxBacklogs = new RubyxBacklogs();
//                    oRubyxBacklogs.setSkippedDate(formattedDate);
//                    oRubyxBacklogs.setStatus("OK");
//                    oBacklogList.add(oRubyxBacklogs);
//                } else {
//
//                }
//                System.out.println("--->  List Size  <------".concat(oBacklogList.size() + ""));
//                for (RubyxBacklogs rubyxBacklogs : oBacklogList) {
//                    formattedDate = rubyxBacklogs.getSkippedDate();
//                    String blobName = objectBaseName + formattedDate + ".json";
//                    Blob blob = storage.get(BlobId.of(bucketName, blobName));
//                    System.out.println("--->  blobName  <------".concat(blobName));
//                    rubyxLoanRenewalFile = "C:\\Omnix\\DataExtraction\\Rubyx\\Loan" + formattedDate.replace("-", "") + ".json";
//                    path = Paths.get(rubyxLoanRenewalFile, new String[0]);
//                    File destinationFile = new File(rubyxLoanRenewalFile);
//                    if (blob != null) {
//                        blob.downloadTo(Paths.get(destinationFile.getPath(), new String[0]));
//                        Reader reader;
//                        try {
//                            reader = Files.newBufferedReader(path);
//                        } catch (IOException iOException) {
//                            continue;
//                        }
//                        RubyxLoanRenewalAlertPayload[] alertList = (RubyxLoanRenewalAlertPayload[]) gson.fromJson(reader, RubyxLoanRenewalAlertPayload[].class);
//                        for (RubyxLoanRenewalAlertPayload alert : alertList) {
//                            RubyxLoanRenewalPayload newRequest = new RubyxLoanRenewalPayload();
//                            newRequest.setAccountOfficer(alert.getPortfolioManager_ID());
//                            newRequest.setBranchCode(alert.getBranch_Code());
//                            newRequest.setCustomerNumber(alert.getCustomer_ID());
//                            newRequest.setProductCode(alert.getLoan_Product_Code());
//                            newRequest.setRenewalAmount(alert.getAmount());
//                            newRequest.setRenewalRating(alert.getRating());
//                            newRequest.setRenewalScore(alert.getScore());
//                            newRequest.setRequestId(alert.getAlert_ID());
//                            newRequest.setEligibilityEndDate(alert.getEligibility_End_Date());
//                            newRequest.setHash(genericService.hashRubyxLoanRenewal(newRequest, "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJydWJ5eCIsInJvbGVzIjoiW1JVQllYLCBDUkVESVRfQlVSRUFVX1ZBTElEQVRJT04sIEFDQ09VTlRfQkFMQU5DRSwgU01TX05PVElGSUNBVElPTiwgVFJBTlNBQ1RJT05fRU1BSUxfQUxFUlQsIExPQU5fT0ZGRVJfRU1BSUxfQUxFUlRdIiwiYXV0aCI6IlZSNmtONHdHamFDYVZtNzBpVkR1WEE9PSIsIkNoYW5uZWwiOiJNb2JpbGUiLCJJUCI6IjA6MDowOjA6MDowOjA6MSIsImlzcyI6IkFjY2lvbiBNaWNyb2ZpbmFuY2UgQmFuayIsImlhdCI6MTY1NTQ2ODI2MywiZXhwIjo2MjUxNDI4NDQwMH0.Pw0TKpXLbV1ny59TD2RXfclAHn_ZdYYDuyCNTQCZFCI"));
//                            rubyxService.processRubyxLoanRenewal("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJydWJ5eCIsInJvbGVzIjoiW1JVQllYLCBDUkVESVRfQlVSRUFVX1ZBTElEQVRJT04sIEFDQ09VTlRfQkFMQU5DRSwgU01TX05PVElGSUNBVElPTiwgVFJBTlNBQ1RJT05fRU1BSUxfQUxFUlQsIExPQU5fT0ZGRVJfRU1BSUxfQUxFUlRdIiwiYXV0aCI6IlZSNmtONHdHamFDYVZtNzBpVkR1WEE9PSIsIkNoYW5uZWwiOiJNb2JpbGUiLCJJUCI6IjA6MDowOjA6MDowOjA6MSIsImlzcyI6IkFjY2lvbiBNaWNyb2ZpbmFuY2UgQmFuayIsImlhdCI6MTY1NTQ2ODI2MywiZXhwIjo2MjUxNDI4NDQwMH0.Pw0TKpXLbV1ny59TD2RXfclAHn_ZdYYDuyCNTQCZFCI", newRequest);
//                        }
//                    }
//                    if (!rubyxBacklogs.getStatus().equalsIgnoreCase("OK")) {
//                        rubyxBacklogs.setStatus("OK");
//                        loanRepository.updateRubyxBacklogs(rubyxBacklogs);
//                    }
//
//                    genericService.generateLog("Rubyx Loan Renewal - Google Cloud Storage", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJydWJ5eCIsInJvbGVzIjoiW1JVQllYLCBDUkVESVRfQlVSRUFVX1ZBTElEQVRJT04sIEFDQ09VTlRfQkFMQU5DRSwgU01TX05PVElGSUNBVElPTiwgVFJBTlNBQ1RJT05fRU1BSUxfQUxFUlQsIExPQU5fT0ZGRVJfRU1BSUxfQUxFUlRdIiwiYXV0aCI6IlZSNmtONHdHamFDYVZtNzBpVkR1WEE9PSIsIkNoYW5uZWwiOiJNb2JpbGUiLCJJUCI6IjA6MDowOjA6MDowOjA6MSIsImlzcyI6IkFjY2lvbiBNaWNyb2ZpbmFuY2UgQmFuayIsImlhdCI6MTY1NTQ2ODI2MywiZXhwIjo2MjUxNDI4NDQwMH0.Pw0TKpXLbV1ny59TD2RXfclAHn_ZdYYDuyCNTQCZFCI", objectBaseName + objectBaseName + ".json", "No Blob in Bucket", "INFO", "");
//                }
//            }
//        } catch (JsonIOException | com.google.gson.JsonSyntaxException ex) {
//            genericService.generateLog("Rubyx Loan Renewal", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJydWJ5eCIsInJvbGVzIjoiW1JVQllYLCBDUkVESVRfQlVSRUFVX1ZBTElEQVRJT04sIEFDQ09VTlRfQkFMQU5DRSwgU01TX05PVElGSUNBVElPTiwgVFJBTlNBQ1RJT05fRU1BSUxfQUxFUlQsIExPQU5fT0ZGRVJfRU1BSUxfQUxFUlRdIiwiYXV0aCI6IlZSNmtONHdHamFDYVZtNzBpVkR1WEE9PSIsIkNoYW5uZWwiOiJNb2JpbGUiLCJJUCI6IjA6MDowOjA6MDowOjA6MSIsImlzcyI6IkFjY2lvbiBNaWNyb2ZpbmFuY2UgQmFuayIsImlhdCI6MTY1NTQ2ODI2MywiZXhwIjo2MjUxNDI4NDQwMH0.Pw0TKpXLbV1ny59TD2RXfclAHn_ZdYYDuyCNTQCZFCI", ex.getMessage(), "Cron Job", "INFO", "");
//        }
//    }

//    @Scheduled(fixedDelay = 300000, initialDelay = 1000)
//    public void disburseRubyxLoanRenewal() {
////        LoanSetup loanType = loanRepository.getLoanTypeUsingCategory("RUBYX");
//        AppUser appUser = loanRepository.getAppUserUsingUsername("Default");
//        //Get all the Rubyx Loan Renewal with completed info. Customer and Guarantor must sign. Guanrantor ID verified and Customer apply
//        List<RubyxLoanRenewal> loanList = loanRepository.getCompletedRubyxLoanRenewal();
//        if (loanList != null) {
//            for (RubyxLoanRenewal oRubyxLoanRenewal : loanList) {
//                Branch branch = loanRepository.getBranchUsingBranchCode(oRubyxLoanRenewal.getBranch());
//                boolean equityContributionOk = false;
//                String accountBalanceRequestId = genericService.generateTransRef("ACB");
//                //Call the Account microservices
//                AccountNumberPayload accBalRequest = new AccountNumberPayload();
//                accBalRequest.setAccountNumber(oRubyxLoanRenewal.getBrightaCommitmentAccount());
//                accBalRequest.setRequestId(accountBalanceRequestId);
//                accBalRequest.setToken(token);
//                accBalRequest.setHash(genericService.hashAccountBalanceRequest(accBalRequest));
//                String accBalRequestJson = gson.toJson(accBalRequest);
//                //Log the request for account balance
//                genericService.generateLog("Rubyx Loan Renewal Disbursement - Account Balance", token, accBalRequestJson, "Cron Job - OFS Request", "INFO", oRubyxLoanRenewal.getRequestId());
//                String accBalResponseJson = accountService.accountBalance(token, accBalRequestJson);
//                genericService.generateLog("Rubyx Loan Renewal Disbursement - Account Balance", token, accBalResponseJson, "Cron Job - OFS Response", "INFO", oRubyxLoanRenewal.getRequestId());
//                AccountBalanceResponsePayload accBalResponse = gson.fromJson(accBalResponseJson, AccountBalanceResponsePayload.class);
//                if (accBalResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode())) {
//                    accBalResponseJson = accountService.accountBalance(token, accBalRequestJson);
//                }
//
//                if (accBalResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
//                    //Check if the customer has the required equity contributions
//                    double tenPercent = 0.1 * Double.valueOf(oRubyxLoanRenewal.getLoanAmount());
//                    double fivePercent = 0.05 * Double.valueOf(oRubyxLoanRenewal.getLoanAmount());
//                    double accountBalance = Double.valueOf(accBalResponse.getAvailableBalance().replace(",", ""));
//                    if (oRubyxLoanRenewal.getCurrentLoanCycle() >= 1 && oRubyxLoanRenewal.getCurrentLoanCycle() <= 5) {
//                        //Get the account balance. Requires 10% equity contribution
//                        if (accountBalance >= tenPercent) {
//                            equityContributionOk = true;
//                        }
//                    }
//
//                    if (oRubyxLoanRenewal.getCurrentLoanCycle() >= 6 && oRubyxLoanRenewal.getCurrentLoanCycle() <= 12) {
//                        //Get the account balance. Requires 5% equity contribution
//                        if (accountBalance >= fivePercent) {
//                            equityContributionOk = true;
//                        }
//                    }
//
//                    if (oRubyxLoanRenewal.getCurrentLoanCycle() > 12) {
//                        equityContributionOk = true;
//                    }
//
//                    //Check if equity contribution is successful
//                    if (equityContributionOk) {
//                        genericService.generateLog("Rubyx Loan Renewal Disbursement - Brighta Commitment Equity Contribution", token, "OK", "Cron Job", "INFO", oRubyxLoanRenewal.getRequestId());
//
//                        //Check if the loan has been disbursed already
//                        Loan disbursedLoan = loanRepository.getLoanUsingRequestId(oRubyxLoanRenewal.getRequestId());
//                        if (disbursedLoan != null) {
//                            oRubyxLoanRenewal.setStatus("FAILED");
//                            oRubyxLoanRenewal.setFailureReason("Loan with request id " + oRubyxLoanRenewal.getRequestId() + " disbursed already");
//                            loanRepository.updateRubyxLoanRenewal(oRubyxLoanRenewal);
//                        } else {
//                            //Take the Admin and Insurance fee. 1% each of approved loan amount
//                            double adminFee = 0.01D * Double.valueOf(oRubyxLoanRenewal.getLoanAmount());
//                            double insuranceFee = 0.01D * Double.valueOf(oRubyxLoanRenewal.getLoanAmount());
//
//                            //Call the disbursement API
//                            DisburseLoanRequestPayload disburse = new DisburseLoanRequestPayload();
//                            disburse.setAmount(oRubyxLoanRenewal.getLoanAmount());
//                            disburse.setAdminFee(true);
//                            disburse.setBranchCode(digitalBranchCode);
//                            disburse.setCategory(oRubyxLoanRenewal.getProductCode());
//                            disburse.setCurrency("NGN");
//                            disburse.setCustomerId(oRubyxLoanRenewal.getCustomer().getCustomerNumber());
//                            disburse.setDrawDownAccount(oRubyxLoanRenewal.getDisbursementAccount());
//                            disburse.setFrequency(String.valueOf(oRubyxLoanRenewal.getTenor()));
//                            disburse.setInterestRate(String.valueOf(oRubyxLoanRenewal.getInterestRate()));
//                            disburse.setInsuranceFee(true);
//                            LocalDate valueDate = LocalDate.now();
//                            LocalDate maturityDate = valueDate.plusMonths(Long.valueOf(oRubyxLoanRenewal.getTenor()));
//                            disburse.setMaturityDate(maturityDate.toString().replace("-", ""));
//                            disburse.setValueDate(valueDate.toString().replace("-", ""));
//                            disburse.setRubyxLoan("AUTO");
//                            String ofsRequest = gson.toJson(disburse);
//
//                            //Generate the OFS Response log
//                            genericService.generateLog("Rubyx Loan Renewal Disbursement", token, ofsRequest, "Cron Job - OFS Request", "INFO", oRubyxLoanRenewal.getRequestId());
//
//                            String middlewareResponse = genericService.postToMiddleware("/loan/disburseDigitalLoan", ofsRequest);
//
//                            //Generate the OFS Response log
//                            genericService.generateLog("Rubyx Loan Renewal Disbursement", token, middlewareResponse, "Cron Job - OFS Response", "INFO", oRubyxLoanRenewal.getRequestId());
//                            LoanResponsePayload responsePayload = gson.fromJson(middlewareResponse, LoanResponsePayload.class);
//                            if (!responsePayload.getResponseCode().equalsIgnoreCase("00")) {
//                                oRubyxLoanRenewal.setStatus("FAILED");
//                                oRubyxLoanRenewal.setFailureReason(responsePayload.getResponseDescription());
//                                loanRepository.updateRubyxLoanRenewal(oRubyxLoanRenewal);
//                            } else {
//                                //Update the Rubyx loan renewal
//                                oRubyxLoanRenewal.setStatus("DISBURSED");
//                                oRubyxLoanRenewal.setFailureReason("");
//                                loanRepository.updateRubyxLoanRenewal(oRubyxLoanRenewal);
//
//                                //Get the loan id from the CBA
//                                String[] contractId = responsePayload.getResponseDescription().split("contractNumber:");
//                                String loanDisbursementId = contractId[0].replace("Successful", "")
//                                        .replace("LD", "").replace("is", "").replace(",", "");
//                                //Update the loan application. Disbursement was successful
//                                Loan newLoan = new Loan();
//                                newLoan.setAppUser(appUser);
//                                newLoan.setAdminFee(String.valueOf(adminFee));
//                                newLoan.setBranch(branch);
//                                newLoan.setCreatedAt(LocalDateTime.now());
//                                newLoan.setCustomer(oRubyxLoanRenewal.getCustomer());
//                                newLoan.setCustomerBusiness("RUBYX LOAN RENEWAL");
//                                newLoan.setDisbursedAt(LocalDate.now());
//                                newLoan.setDisbursementAccount(oRubyxLoanRenewal.getDisbursementAccount());
//                                newLoan.setFirstRepaymentDate(LocalDate.now());
//                                newLoan.setInsuranceFee(String.valueOf(insuranceFee));
//                                newLoan.setLiquidatedAt(LocalDate.parse("1900-01-01"));
//                                newLoan.setLoanAmountApproved(new BigDecimal(oRubyxLoanRenewal.getRenewalAmount()));
//                                newLoan.setLoanAmountRequested(new BigDecimal(oRubyxLoanRenewal.getLoanAmount()));
//                                newLoan.setLoanDisbursementId(loanDisbursementId != null ? "LD" + loanDisbursementId.trim() : "");
//                                newLoan.setLoanId(loanDisbursementId != null ? "LD" + loanDisbursementId.trim() : "");
//                                newLoan.setLoanPurpose("RUBYX LOAN RENEWAL");
////                                newLoan.setLoanSetup(loanType);
//                                newLoan.setLoanTenor(oRubyxLoanRenewal.getTenor());
//                                newLoan.setMaturedAt(LocalDate.parse("1900-01-01"));
//                                newLoan.setMobileNumber(oRubyxLoanRenewal.getMobileNumber());
//                                newLoan.setMonthlyRepayment(new BigDecimal(0));
//                                newLoan.setProductCode(oRubyxLoanRenewal.getProductCode());
//                                newLoan.setRequestId(oRubyxLoanRenewal.getRequestId());
//                                newLoan.setStatus("SUCCESS");
//                                newLoan.setTotalRepayment(BigDecimal.ZERO);
//                                newLoan.setInterestRate(BigDecimal.ZERO);
//                                newLoan.setTimePeriod(genericService.getTimePeriod());
//                                newLoan.setSelectionScore(oRubyxLoanRenewal.getRenewalScore());
//                                newLoan.setSelectionScoreRating(oRubyxLoanRenewal.getRenewalRating());
//                                newLoan.setLimitRange("0");
//                                newLoan.setMsmeScore(String.valueOf("0"));
//                                loanRepository.createLoan(newLoan);
//                            }
//                        }
//                    } else {
//                        genericService.generateLog("Rubyx Loan Renewal Disbursement - Brighta Commitment Equity Contribution", token, "NO", "Cron Job", "INFO", oRubyxLoanRenewal.getRequestId());
//                        //Equity contribution not okay
//                        oRubyxLoanRenewal.setStatus("FAILED");
//                        oRubyxLoanRenewal.setFailureReason("Failed to Execute Brighta Commitment Deposit Contribution");
//                        loanRepository.updateRubyxLoanRenewal(oRubyxLoanRenewal);
//                    }
//                } else {
//                    genericService.generateLog("Rubyx Loan Renewal Disbursement - Account Balance", token, accBalResponseJson, "Cron Job", "INFO", oRubyxLoanRenewal.getRequestId());
//                    //Equity contribution not okay
//                    oRubyxLoanRenewal.setStatus("FAILED");
//                    oRubyxLoanRenewal.setFailureReason("Failed to Execute Brighta Commitment Deposit Contribution");
//                    loanRepository.updateRubyxLoanRenewal(oRubyxLoanRenewal);
//                }
//            }
//        }
//        genericService.generateLog("Rubyx Loan Renewal Disbursement", token, "No record for set for disbursement", "Cron Job", "INFO", "");
//    }

//    @Scheduled(fixedDelay = 60000)
    @Scheduled(fixedDelay = 300000, initialDelay = 1000)
//    public void fetchCreditBureauDetails() {
//        //Get all the pending Rubyx loan renewal with Credit Bureau search not done
//        List<RubyxLoanRenewal> loanList = loanRepository.getRubyxLoanRenewalForCreditBureau();
//        if (loanList != null) {
//            for (RubyxLoanRenewal lon : loanList) {
//                //Check if the customer has loan running already
//                List<Loan> loanRecord = loanRepository.getLoanUsingCustomer(lon.getCustomer());
//                boolean loanExist = false;
//                if (loanRecord != null) {
//                    for (Loan l : loanRecord) {
//                        if (!l.getStatus().equalsIgnoreCase("LIQUIDATE") && !l.getStatus().equalsIgnoreCase("DECLINED")) {
//                            loanExist = true;
//                        }
//                    }
//                }
//
//                if (loanExist) {
//                    lon.setStatus("FAILED");
//                    lon.setFailureReason("Customer has active loan running. Check the Loan table for details");
//                    loanRepository.updateRubyxLoanRenewal(lon);
//                } else {
//                    String requestId = genericService.generateTransRef("CRC");
//                    //Check if No hit from Credit Bureau
//                    if (lon.isCreditBureauNoHit()) {
//                        genericService.generateLog("Rubyx Loan Renewal", token, "No Hit for customer " + lon.getCustomerNumber(), "Cron Job - Credit Bureau", "INFO", requestId);
//                        //Check if the SMS is sent to customer
//                        if (!lon.isCustomerSmsSent()) {
//                            //This is NO-HIT status. Send SMS
//                            NotificationPayload smsRequest = new NotificationPayload();
//                            smsRequest.setInterestRate(Double.valueOf(lon.getInterestRate()));
//                            smsRequest.setTenor(Integer.valueOf("3")); //Tenor is not available at this point, hence defaulted to the min tenor
//                            smsRequest.setAmountApproved(new BigDecimal(lon.getRenewalAmount()));
//                            smsRequest.setMobileNumber(lon.getMobileNumber());
//                            smsRequest.setRequestId(requestId);
//                            smsRequest.setToken(token);
//                            genericService.sendRubyxLoanRenewalSMS(smsRequest);
//
//                            //Send email to contact center
//                            NotificationPayload emailRequest = new NotificationPayload();
//                            emailRequest.setRecipientName(lon.getCustomer().getLastName());
//                            emailRequest.setRecipientEmail("bokon@accionmfb.com");
//                            emailRequest.setLastName(lon.getCustomer().getLastName());
//                            emailRequest.setOtherName(lon.getCustomer().getOtherName());
//                            emailRequest.setLoanAmount(lon.getRenewalAmount());
//                            emailRequest.setLoanTenor("3"); //Tenor is not available at this point, hence defaulted to the min tenor
//                            emailRequest.setLoanType(lon.getProductCode());
//                            emailRequest.setMobileNumber(lon.getMobileNumber());
//                            emailRequest.setTotalLoans(String.valueOf(0));
//                            emailRequest.setTotalPerformingLoans(String.valueOf(0));
//                            emailRequest.setTotalLoanBalance(String.valueOf(0));
//                            emailRequest.setTotalPerformingLoanBalance(String.valueOf(0));
//                            emailRequest.setRequestId(requestId);
//                            emailRequest.setToken(token);
//                            genericService.sendRubyxLoanRenewalEmail(emailRequest);
//
//                            //Update the DB
//                            lon.setCustomerSmsSent(true);
//                            lon.setCreditBureauSearchDone(true);
//                            loanRepository.updateRubyxLoanRenewal(lon);
//                        }
//
//                        //Set the credit bureau search as done
//                        lon.setCreditBureauSearchDone(true);
//                        loanRepository.updateRubyxLoanRenewal(lon);
//                    } else {
//                        //Create the request payload
//                        CreditBureauRequestPayload creditBureauPayload = new CreditBureauRequestPayload();
//                        creditBureauPayload.setMobileNumber(lon.getMobileNumber());
//                        creditBureauPayload.setBvn(lon.getBvn());
//                        creditBureauPayload.setSearchType("CREDITHISTORY");
//                        creditBureauPayload.setRequestId(requestId);
//                        creditBureauPayload.setToken(token);
//                        creditBureauPayload.setHash(genericService.hashCreditBureauValidationRequest(creditBureauPayload));
//                        String creditBureauRequestPayload = gson.toJson(creditBureauPayload);
//                        genericService.generateLog("Rubyx Loan Renewal", token, creditBureauRequestPayload, "Cron Job - Credit Bureau Request", "INFO", requestId);
//                        String creditBureauResponseJson = creditBureauService.creditBureauValidation(token, creditBureauRequestPayload);
//                        genericService.generateLog("Rubyx Loan Renewal", token, creditBureauResponseJson, "Cron Job - Credit Bureau Response", "INFO", requestId);
//                        CreditBureauResponsePayload creditBureauResponsePayload = gson.fromJson(creditBureauResponseJson, CreditBureauResponsePayload.class);
//
//                        switch (creditBureauResponsePayload.getResponseCode()) {
//                            case "03": {
//                                //This is a NO HIT
//                                if (!lon.isCustomerSmsSent()) {
//                                    //This is NO-HIT status. Send SMS
//                                    NotificationPayload smsRequest = new NotificationPayload();
//                                    smsRequest.setInterestRate(Double.valueOf(lon.getInterestRate()));
//                                    smsRequest.setTenor(Integer.valueOf("3")); //Tenor is not available at this point, hence defaulted to the min tenor
//                                    smsRequest.setAmountApproved(new BigDecimal(lon.getRenewalAmount()));
//                                    smsRequest.setMobileNumber(lon.getMobileNumber());
//                                    smsRequest.setRequestId(requestId);
//                                    smsRequest.setToken(token);
//                                    genericService.sendRubyxLoanRenewalSMS(smsRequest);
//
//                                    //Send email to contact center
//                                    NotificationPayload emailRequest = new NotificationPayload();
//                                    emailRequest.setRecipientName(lon.getCustomer().getLastName());
//                                    emailRequest.setRecipientEmail("bokon@accionmfb.com");
//                                    emailRequest.setLastName(lon.getCustomer().getLastName());
//                                    emailRequest.setOtherName(lon.getCustomer().getOtherName());
//                                    emailRequest.setLoanAmount(lon.getRenewalAmount());
//                                    emailRequest.setLoanTenor("3"); //Tenor is not available at this point, hence defaulted to the min tenor
//                                    emailRequest.setLoanType(lon.getProductCode());
//                                    emailRequest.setMobileNumber(lon.getMobileNumber());
//                                    emailRequest.setTotalLoans(String.valueOf(0));
//                                    emailRequest.setTotalPerformingLoans(String.valueOf(0));
//                                    emailRequest.setTotalLoanBalance(String.valueOf(0));
//                                    emailRequest.setTotalPerformingLoanBalance(String.valueOf(0));
//                                    emailRequest.setRequestId(requestId);
//                                    emailRequest.setToken(token);
//                                    genericService.sendRubyxLoanRenewalEmail(emailRequest);
//
//                                    //Update the DB
//                                    lon.setCustomerSmsSent(true);
//                                    lon.setCreditBureauNoHit(true);
//                                    lon.setCreditBureauSearchDone(true);
//                                    loanRepository.updateRubyxLoanRenewal(lon);
//                                }
//
//                                //Set the credit bureau search as done
//                                lon.setCreditBureauSearchDone(true);
//                                lon.setCreditBureauNoHit(true);
//                                loanRepository.updateRubyxLoanRenewal(lon);
//                            }
//                            case "23": {
//                                //This is corrupt data status
//                            }
//                            case "00": {
//                                //This is success status
//                                boolean hasNonPerforming = false;
//                                double totalOutstandingBalance = 0D;
//                                double totalPerformingBalance = 0D;
//                                int totalLoans = 0;
//                                int totalPerformingLoans = 0;
//                                List<CreditBureauPayload> creditHistoryList = creditBureauResponsePayload.getCreditFacilityHistory();
//                                if (creditHistoryList != null) {
//                                    for (CreditBureauPayload hist : creditHistoryList) {
//                                        totalOutstandingBalance += Double.valueOf(hist.getCurrentBalance().replace(",", ""));
//                                        if (hist.getAssetClassification().equalsIgnoreCase("Performing")
//                                                || hist.getAssetClassification().equalsIgnoreCase("Watch")) {
//                                            totalPerformingBalance += Double.valueOf(hist.getCurrentBalance().replace(",", ""));
//                                            totalPerformingLoans += 1;
//                                        }
//
//                                        //Check if there are Non Performing Loan
//                                        if (hist.getAssetClassification().equalsIgnoreCase("Sub Standard")
//                                                || hist.getAssetClassification().equalsIgnoreCase("Watchlist")
//                                                || hist.getAssetClassification().equalsIgnoreCase("Doubtful")
//                                                || hist.getAssetClassification().equalsIgnoreCase("Lost")) {
//                                            hasNonPerforming = true;
//                                        }
//
//                                        totalLoans += 1;
//                                    }
//
//                                    //Check if all loans are performing
//                                    if (hasNonPerforming) {
//                                        //Has non performing loan. Update the status
//                                        lon.setStatus("FAILED");
//                                        lon.setFailureReason("HAS NON PERFORMING LOAN");
//                                        lon.setTotalLoanBalance(BigDecimal.valueOf(totalOutstandingBalance));
//                                        lon.setTotalPerformingBalance(BigDecimal.valueOf(totalPerformingBalance));
//                                        lon.setNoOfPerformingLoans(totalPerformingLoans);
//                                        lon.setNoOfTotalLoans(totalLoans);
//                                        lon.setCreditBureauSearchDone(true);
//                                        loanRepository.updateRubyxLoanRenewal(lon);
//                                    } else {
//                                        double loanAmount = totalPerformingBalance * 100D; //Loan Amount * 100
//                                        double loanPercent = loanAmount / Double.valueOf(lon.getRenewalAmount());
//                                        if (loanPercent > 400) {
//                                            lon.setStatus("FAILED");
//                                            lon.setFailureReason("Total Performing Balance " + totalPerformingBalance + ". Loan Amount is " + lon.getRenewalAmount());
//                                            lon.setTotalLoanBalance(BigDecimal.valueOf(totalOutstandingBalance));
//                                            lon.setTotalPerformingBalance(BigDecimal.valueOf(totalPerformingBalance));
//                                            lon.setNoOfPerformingLoans(totalPerformingLoans);
//                                            lon.setNoOfTotalLoans(totalLoans);
//                                            lon.setCreditBureauSearchDone(true);
//                                            loanRepository.updateRubyxLoanRenewal(lon);
//                                        } else {
//                                            //Check if the total balances of the performing loans is > 300% of the total outstanding
//                                            double performingBalancePercent = totalOutstandingBalance * 100D;
//                                            double balancePercent = performingBalancePercent / Double.valueOf(lon.getRenewalAmount());
//                                            if (balancePercent > 300) {
//                                                lon.setStatus("FAILED");
//                                                lon.setFailureReason("Total Performing Balance " + totalPerformingBalance + ". Loan Amount is " + lon.getRenewalAmount());
//                                                lon.setTotalLoanBalance(BigDecimal.valueOf(totalOutstandingBalance));
//                                                lon.setTotalPerformingBalance(BigDecimal.valueOf(totalPerformingBalance));
//                                                lon.setNoOfPerformingLoans(totalPerformingLoans);
//                                                lon.setNoOfTotalLoans(totalLoans);
//                                                lon.setCreditBureauSearchDone(true);
//                                                loanRepository.updateRubyxLoanRenewal(lon);
//                                            } else {
//                                                //Good to go
//                                                if (!lon.isCustomerSmsSent()) {
//                                                    NotificationPayload smsRequest = new NotificationPayload();
//                                                    smsRequest.setInterestRate(Double.valueOf(lon.getInterestRate()));
//                                                    smsRequest.setTenor(Integer.valueOf("3")); //Tenor is not available at this point, hence defaulted to the min tenor
//                                                    smsRequest.setAmountApproved(new BigDecimal(lon.getRenewalAmount()));
//                                                    smsRequest.setMobileNumber(lon.getMobileNumber());
//                                                    smsRequest.setRequestId(requestId);
//                                                    smsRequest.setToken(token);
//                                                    genericService.sendRubyxLoanRenewalSMS(smsRequest);
//
//                                                    //Send email to contact center
//                                                    NotificationPayload emailRequest = new NotificationPayload();
//                                                    emailRequest.setRecipientName(lon.getCustomer().getLastName());
//                                                    emailRequest.setRecipientEmail("bokon@accionmfb.com");
//                                                    emailRequest.setLastName(lon.getCustomer().getLastName());
//                                                    emailRequest.setOtherName(lon.getCustomer().getOtherName());
//                                                    emailRequest.setLoanAmount(lon.getRenewalAmount());
//                                                    emailRequest.setLoanTenor("3"); //Tenor is not available at this point, hence defaulted to the min tenor
//                                                    emailRequest.setLoanType(lon.getProductCode());
//                                                    emailRequest.setMobileNumber(lon.getMobileNumber());
//                                                    emailRequest.setTotalLoans(String.valueOf(totalLoans));
//                                                    emailRequest.setTotalPerformingLoans(String.valueOf(totalPerformingLoans));
//                                                    emailRequest.setTotalLoanBalance(String.valueOf(totalOutstandingBalance));
//                                                    emailRequest.setTotalPerformingLoanBalance(String.valueOf(totalPerformingBalance));
//                                                    emailRequest.setRequestId(requestId);
//                                                    emailRequest.setToken(token);
//                                                    genericService.sendRubyxLoanRenewalEmail(emailRequest);
//
//                                                    //Update the DB
//                                                    lon.setStatus("SUCCESS");
//                                                    lon.setCustomerSmsSent(true);
//                                                    lon.setTotalLoanBalance(BigDecimal.valueOf(totalOutstandingBalance));
//                                                    lon.setTotalPerformingBalance(BigDecimal.valueOf(totalPerformingBalance));
//                                                    lon.setNoOfPerformingLoans(totalPerformingLoans);
//                                                    lon.setNoOfTotalLoans(totalLoans);
//                                                    lon.setCreditBureauSearchDone(true);
//                                                    loanRepository.updateRubyxLoanRenewal(lon);
//                                                }
//                                                //Update the DB
//                                                lon.setStatus("SUCCESS");
//                                                lon.setTotalLoanBalance(BigDecimal.valueOf(totalOutstandingBalance));
//                                                lon.setTotalPerformingBalance(BigDecimal.valueOf(totalPerformingBalance));
//                                                lon.setNoOfPerformingLoans(totalPerformingLoans);
//                                                lon.setNoOfTotalLoans(totalLoans);
//                                                lon.setCreditBureauSearchDone(true);
//                                                loanRepository.updateRubyxLoanRenewal(lon);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        genericService.generateLog("Rubyx Loan Renewal", token, "No record set for Credit Bureau search", "Cron Job", "INFO", "");
//    }

//    @Scheduled(fixedDelay = 80000)
//    public void updateRubyxLoanRenewalSignOff() {
//        List<RubyxLoanRenewal> loanList = loanRepository.getSignedOffRubyxLoanRenewal();
//        if (loanList != null) {
//            loanList.stream().map(lon -> {
//                lon.setStatus("ACCEPTED");
//                return lon;
//            }).forEachOrdered(lon -> {
//                loanRepository.updateRubyxLoanRenewal(lon);
//            });
//        }
//    }

    //    @Scheduled(fixedDelay = 10000)
//    public void artisanLoanRenewal() {
//        try {
//            AppUser defaultUser = loanRepository.getAppUserUsingUsername("Default");
//            UsernamePayload userPayload = new UsernamePayload();
//            userPayload.setUsername(defaultUser.getUsername());
//            userPayload.setPassword(defaultUser.getPassword());
//            String userJson = gson.toJson(userPayload);
//            String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJib2tvbiIsInJvbGVzIjoiW0FDQ09VTlRfQkFMQU5DRSwgQUNDT1VOVF9TVEFURU1FTlQsIFdBTExFVF9BQ0NPVU5UX09QRU5JTkcsIEFJUlRJTUVfT1RIRVJTLCBBSVJUSU1FX0RBVEFfREVUQUlMUywgREFUQV9QTEFOLCBCVk5fVkFMSURBVElPTiwgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBDQUJMRV9UVl9ERVRBSUxTLCBDQUJMRV9UVl9CSUxMRVIsIElERU5USVRZX1ZBTElEQVRJT04sIFdBTExFVF9CQUxBTkNFLCBXQUxMRVRfREVUQUlMUywgV0FMTEVUX0NVU1RPTUVSLCBDUkVBVEVfV0FMTEVULCBDTE9TRV9XQUxMRVQsIFdBTExFVF9BSVJUSU1FX1NFTEYsIFdBTExFVF9BSVJUSU1FX09USEVSUywgV0FMTEVUX0RBVEFfU0VMRiwgV0FMTEVUX0RBVEFfT1RIRVJTLCBXQUxMRVRfQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBXQUxMRVRfRUxFQ1RSSUNJVFlfQklMTCwgV0FMTEVUX1RPX1dBTExFVF9GVU5EU19UUkFOU0ZFUiwgV0FMTEVUX1RPX0FDQ09VTlRfRlVORFNfVFJBTlNGRVIsIFdBTExFVF9UT19JTlRFUl9CQU5LX1RSQU5TRkVSLCBJTlRFUl9CQU5LX1RPX1dBTExFVF9GVU5EU19UUkFOU0ZFUiwgV0FMTEVUX0lOVEVSX0JBTktfTkFNRV9FTlFVSVJZLCBDT05WRVJUX1dBTExFVF9UT19BQ0NPVU5ULCBJTkRJVklEVUFMX1dJVEhfQlZOLCBJTkRJVklEVUFMX1dJVEhPVVRfQlZOLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIFJFVkVSU0VfTE9DQUxfRlVORFNfVFJBTlNGRVIsIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEVMRUNUUklDSVRZX0JJTExfUEFZTUVOVCwgRUxFQ1RSSUNJVFlfQklMTF9ERVRBSUxTLCBFTEVDVFJJQ0lUWV9CSUxMRVJTLCBDUkVESVRfQlVSRUFVX1ZBTElEQVRJT04sIFNNU19OT1RJRklDQVRJT04sIFRSQU5TQUNUSU9OX0VNQUlMX0FMRVJULCBMT0FOX09GRkVSX0VNQUlMX0FMRVJULCBMT0FOX0dVQVJBTlRPUl9FTUFJTF9BTEVSVCwgUE9TVElOR19SRVNUUklDVElPTiwgUE9TVElOR19SRVNUUklDVElPTiwgQ0FSRF9SRVFVRVNULCBOSUJTU19RUl9QQVlNRU5ULCBCT09LX0RJR0lUQUxfTE9BTiwgQVVUSF9TRUNVUklUWV9RVUVTVElPTiwgVEVMTEVSLCBVUERBVEVfQ1VTVE9NRVJfQlZOLCBBQ0NFUFRfRElHSVRBTF9MT0FOLCBESVNCVVJTRV9ESUdJVEFMX0xPQU4sIExPQU5fVFlQRV9MSVNUSU5HLCBXQUxMRVRfQUNDT1VOVF9PUEVOSU5HLCBCT09LX0FSVElTQU5fTE9BTiwgUEVORElOR19BUlRJU0FOX0xPQU4sIERJU0JVUlNFX0FSVElTQU5fTE9BTiwgQVVUSF9BUlRJU0FOX0xPQU4sIFJFTkVXX0FSVElTQU5fTE9BTiwgTE9BTl9TRVRVUF0iLCJlbmNyeXB0aW9uS2V5Ijoiczd0cEtab01sUm1pdUlXIiwiQ2hhbm5lbCI6Ik1vYmlsZSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjM3ODI5OTU2LCJleHAiOjE2Mzc4ODk5NTZ9.WiEhw2f4nUVJfc5qJnzn8ef_mFv2tEkGz0N38aP219w";
//
//            LoanRenewalRequestPayload loanPayload = new LoanRenewalRequestPayload();
//            String requestId = genericService.generateRequestId();
//            loanPayload.setRequestId(requestId);
//
//            StringJoiner rawString = new StringJoiner(":");
//            rawString.add(requestId);
//            String hashValue = genericService.encryptString(rawString.toString(), token);
//            loanPayload.setHash(hashValue);
//
//            artisanService.processArtisanLoanRenewal(token, loanPayload);
//        } catch (Exception ex) {
//            genericService.generateLog("Artisan Loan Renewal", "", "Scheduled Service Failed to Run", "API Request", "INFO", "");
//        }
//    }
//
//        @Scheduled(fixedDelay = 80000)

//    public void updateRubyxLoanBvn() {
//        String userCredentials = jwtToken.getUserCredentialFromToken(token);
//        List<RubyxLoanRenewal> loanList = loanRepository.getBvn();
//        if (loanList != null) {
//            for (RubyxLoanRenewal loan : loanList) {
//                //get customer bvn
//                String ofsRequest = customerVersion.trim().replace("/I/", "/S/") + "," + userCredentials
//                        + "/" + "/NG0010001," + loan.getCustomerNumber();;
//                String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
//                //Generate the OFS Response log
//                genericService.generateLog("Customer Upload", token, newOfsRequest, "OFS Request", "INFO", loan.getRequestId());
//                //Post to T24
//                String bvnResponse = genericService.postToT24(ofsRequest);
//                String validationResponse = genericService.validateT24Response(bvnResponse);
//                if (validationResponse != null) {
//                    //Log the error
//                    genericService.generateLog("Rubyx Loan Renewal", token, bvnResponse, "API Response", "INFO", loan.getRequestId());
//                    loan.setStatus("FAILED");
//                    loan.setFailureReason("Unable to retrieve customer details");
//                    loanRepository.updateRubyxLoanRenewal(loan);
//                } else {
//                    String bvn = genericService.getTextFromOFSResponse(bvnResponse, "BVN:1:1");
//                    if (bvn == null || bvn.equalsIgnoreCase("")) {
//                        loan.setStatus("FAILED");
//                        loan.setFailureReason(messageSource.getMessage("appMessages.bvn.notexist", new Object[0], Locale.ENGLISH));
//                        loanRepository.updateRubyxLoanRenewal(loan);
//                    } else {
//                        loan.setBvn(bvn);
//                        loan.setStatus("SUCCESS");
//                        loan.setFailureReason("");
//                        loanRepository.updateRubyxLoanRenewal(loan);
//                    }
//                }
//            }
//        }
//    }

    public void chargeCardForLoanRepayment() {
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        // List<Loan> loanList = loanRepository.get;
    }


    //    @Scheduled(cron = "0 0 0 * * *") // Run at 12 AM every day


//    @Scheduled(fixedRate = 5000)
    private void chargePaystackForRepaymentAfter3Days() throws UnirestException, ParseException {
        LockAmountDto request = new LockAmountDto();

        request.setAccountNumber("9991030330");
        request.setAmount("500000");
        log.info("lockdown amount payload {}", request);

        EarlyRepaymentRequestPayload requestPayload = new EarlyRepaymentRequestPayload();
        LocalDate currentDate = LocalDate.now();

    }



}
