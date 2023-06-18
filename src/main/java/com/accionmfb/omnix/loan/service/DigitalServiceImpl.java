/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.constant.LoanCategory;
import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.model.*;
import com.accionmfb.omnix.loan.payload.*;
import com.accionmfb.omnix.loan.payload.DigitalLoanHistoryResponseList;
import com.accionmfb.omnix.loan.repository.LoanRepository;
import com.accionmfb.omnix.loan.repository.SmsRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author bokon
 */
@Service
@Slf4j
public class DigitalServiceImpl implements DigitalService {

    private final String PAYSTACK_KEY = "sk_test_49a61dd20304a342f17f953d8424530c49c169ed";
    private final String PAYSTACK_URL = "https://api.paystack.co/transaction/verify/";

    private final String PAYSTACK_CHARGE_CARD_URL = "https://api.paystack.co/transaction/charge_authorization";

    private final String PAYSTACK_CHARGE_CARD_KEY = "sk_test_49a61dd20304a342f17f953d8424530c49c169ed";


    @Autowired
    LoanRepository loanRepository;
    @Autowired
    MessageSource messageSource;
    @Autowired
    GenericService genericService;
    @Autowired
    IdentityService identityService;
    @Autowired
    BVNService bvnService;
    @Autowired
    CreditBureauService creditBureauService;
    @Autowired
    DigitalService digitalService;
    @Autowired
    SmsRepository smsRepository;
    @Autowired
    FundsTransferService ftService;
    @Autowired
    AccountService accountService;
    @Autowired
    JwtTokenUtil jwtToken;
    @Value("${omnix.digital.branch.code}")
    private String digitalBranchCode;
    @Value("${omnix.digital.loan.code}")
    private String digitalLoanCode;
    @Value("${omnix.loan.threshold.deliquent.loans}")
    private BigDecimal thresholdForDeliquentLoans;
    @Value("${omnix.loan.threshold.overdue.amount}")
    private BigDecimal thresholdForOverdueAmount;
    @Value("${omnix.mono.api.url}")
    private String monoApiUrl;
    @Value("${omnix.mono.secret.key}")
    private String monoSecretKey;
    @Value("${omnix.indicina.api.url}")
    private String indicinaApiUrl;
    @Value("${omnix.indicina.secret.key}")
    private String indicinaSeecretKey;
    @Value("${omnix.indicina.customer.id}")
    private String indicinaCustomerId;
    @Value("${omnix.indicina.client.id}")
    private String indicinaClientId;
    @Value("${omnix.middleware.active}")
    private String middlewareActiveLoanAPI;

    @Autowired
    Gson gson;
    @Autowired
    ApplicationContext applicationContext;
    private static final Double DEBT_SERVICE_RATIO = Double.valueOf("33.3") / 100;
    private final BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder();
    private static final Logger LOGGER = Logger.getLogger(DigitalServiceImpl.class.getName());

//    @Override

    public boolean validateLoanApplicantDetailsPayload(String token, LoanApplicantDetailsPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getGeolocation());
        rawString.add(requestPayload.getPicture());
        rawString.add(requestPayload.getPhonebook());
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    public String processLoanApplicantDetails(String token, LoanApplicantDetailsPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String response = "";
        LoanApplicantDetailsPayload responsePayload = new LoanApplicantDetailsPayload();

        String requestJson = gson.toJson(requestPayload);
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            genericService.generateLog("Digital Loan Applicant Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Digital Loan Applicant Details", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        if (customer == null) {
            genericService.generateLog("Digital Loan Applicant Details", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Loan Applicant Details", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
        return "";
    }

    @Override
    public boolean validateDigitalLoanBookingPayload(String token, DigitalLoanRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getBvn().trim());
        rawString.add(requestPayload.getLoanType());
        rawString.add(requestPayload.getLoanAmount().trim());
        rawString.add(String.valueOf(requestPayload.getLoanTenor()).trim());
        rawString.add(requestPayload.getLoanPurpose().trim());
        rawString.add(requestPayload.getDisbursementAccount().trim());
        rawString.add(requestPayload.getBusinessType().trim());
        rawString.add(requestPayload.getBusinessName().trim());
        rawString.add(requestPayload.getBusinessAddress().trim());
        rawString.add(requestPayload.getMonoAccountCode().trim());
        rawString.add(requestPayload.getMaritalStatus().trim());
        rawString.add(requestPayload.getNameOfSpouse().trim());
        rawString.add(requestPayload.getNoOfDependent().trim());
        rawString.add(requestPayload.getTypeOfResidence().trim());
        rawString.add(requestPayload.getRentPerYear().trim());
        rawString.add(requestPayload.getYearOfResidency().trim());
        rawString.add(requestPayload.getImei().trim());
        rawString.add(requestPayload.getRequestId().trim());

//        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase("decryptedString");
    }

    @Override
    @HystrixCommand(fallbackMethod = "digitalLoanBookingFallback")
    public String processDigitalLoanBooking(String token, DigitalLoanRequestPayload requestPayload) {

        final OmniResponsePayload[] errorResponse = {new OmniResponsePayload()};
        LoanResponsePayload loanResponse = new LoanResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String response = "";
        Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        if (customer == null) {
            //Log the error
            genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse[0].setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse[0]);
        }

        //Set the customer number
        String customerNumber = customer.getCustomerNumber().trim();
        //Check the status of the customer
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            //Log the error
            genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse[0].setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
            errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse[0]);
        }

        //validate IMEI FOR MOBILE CHANNEL
        if ("MOBILE".equalsIgnoreCase(channel)) {
            boolean imeiMatch = bCryptEncoder.matches(requestPayload.getImei(), customer.getImei());
            if (!imeiMatch) {
                genericService.generateLog("Customer Details", token, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                genericService.createUserActivity("", "Validate IMEI", "appMessages.customer.imei.invalid", channel, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse[0].setResponseCode(ResponseCodes.IMEI_MISMATCH.getResponseCode());
                errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse[0]);
            }
        }
        //Check for customer account for the disbursement
        Account disbursementAccount = loanRepository.getCustomerAccount(customer, requestPayload.getDisbursementAccount());
        if (disbursementAccount == null) {
            //Log the error
            genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse[0].setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
            errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse[0]);
        }

        // check if account is a saveBrighta Account
//            Account accountType = loanRepository.getAccountUsingAccountNumber(requestPayload.getDisbursementAccount());
//   
//            if (!accountType.getProduct().getProductCode().equals("14")) {
//                //Log the error
//                genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("Account is not a SaveBrighta Account", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("Account is not a SaveBrighta Account", new Object[0], Locale.ENGLISH), requestPayload.getDisbursementAccount(), 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("Account is not a SaveBrighta Account", new Object[0], Locale.ENGLISH));
//                return gson.toJson(errorResponse);
//            }
        //Check the channel information
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(disbursementAccount.getAccountNumber(), "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse[0].setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse[0]);
        }

        //Check if there is a loan record
        List<Loan> loanRecord = loanRepository.getLoanUsingCustomer(customer);

//        List<Loan> loanRecordT24 = loanRepository.getActiveLoanUsingCustomer(customer);

//        LoanBalanceResponsePayload loanBalance = digitalService.getLoanBalance(String.valueOf(disbursementAccount), token);

        boolean loanExist = false;
        if (loanRecord != null) {
            for (Loan l : loanRecord) {
                if (!l.getStatus().equalsIgnoreCase("LIQUIDATE") && !l.getStatus().equalsIgnoreCase("DECLINED")) {
                    loanExist = true;
                }
            }
        }

        if (loanExist) {
            //Log the error
            genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.pending", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.pending", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse[0].setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
            errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.pending", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse[0]);
        }

        //Check the loan type selected
        LoanSetup loanType = loanRepository.getLoanTypeUsingCategory(requestPayload.getLoanType());
        if (loanType == null) {
            //Log the error
            genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.noexist", new Object[]{requestPayload.getLoanType()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.noexist", new Object[]{requestPayload.getLoanType()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse[0].setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.noexist", new Object[]{requestPayload.getLoanType()}, Locale.ENGLISH));
            return gson.toJson(errorResponse[0]);
        }

        //Check if the customer has existing loan in the domicile branch
        Object loanBalances = genericService.getLoanBalances(customer.getBranch().getBranchCode(), customer.getCustomerNumber(), userCredentials);
        if (!(loanBalances instanceof String)) {
            List<PortfolioPayload> loanList = (List<PortfolioPayload>) loanBalances;
            if (loanList != null && !loanList.isEmpty()) {

                //Log the error
                genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.exist", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.exist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse[0].setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.exist", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
        }
        Loan newLoan = new Loan();
        String loanId = genericService.generateTransRef("BRL");

        //Persist the Loan Booking initially in this thread
        CompletableFuture<String> persistLoanRecordInitially = CompletableFuture.supplyAsync(() -> {
            newLoan.setAppUser(appUser);
            newLoan.setCreatedAt(LocalDateTime.now());
            newLoan.setCustomer(customer);
            newLoan.setBusinessType(requestPayload.getBusinessType());
            newLoan.setDisbursedAt(LocalDate.parse("1900-01-01"));
            newLoan.setDisbursementAccount(requestPayload.getDisbursementAccount());
            newLoan.setBusinessAddress(requestPayload.getBusinessAddress());
            newLoan.setBusinessName(requestPayload.getBusinessName());
            newLoan.setMaritalStatus(requestPayload.getMaritalStatus());
            newLoan.setNameOfSpouse(requestPayload.getNameOfSpouse());
            newLoan.setNoOfDependent(requestPayload.getNoOfDependent());
            newLoan.setTypeOfResidence(requestPayload.getTypeOfResidence());
            newLoan.setRentPerYear(requestPayload.getRentPerYear());
            newLoan.setYearOfResidence(requestPayload.getYearOfResidency());
            newLoan.setCustomerBusiness(requestPayload.getCustomerBusiness());

            newLoan.setFirstRepaymentDate(LocalDate.now()); //Update in Second Thread for Loan Booking
            newLoan.setLiquidatedAt(LocalDate.parse("1900-01-01"));
            newLoan.setLoanAmountApproved(new BigDecimal(0)); //Update in Second Thread for Loan Booking
            newLoan.setLoanAmountRequested(new BigDecimal(requestPayload.getLoanAmount()));
            newLoan.setLoanDisbursementId("");

            newLoan.setLoanId(loanId);
            newLoan.setLoanPurpose(requestPayload.getLoanPurpose());
            newLoan.setLoanSetup(loanType);
            newLoan.setLoanTenor("");
            newLoan.setMaturedAt(LocalDate.parse("1900-01-01"));
            newLoan.setMobileNumber(requestPayload.getMobileNumber());
            newLoan.setMonthlyRepayment(new BigDecimal(0));
            newLoan.setRequestId(requestPayload.getRequestId());
            newLoan.setStatus("STAGE1"); //Update in Second Thread for Loan Booking
            newLoan.setTotalRepayment(BigDecimal.ZERO);
            newLoan.setInterestRate(BigDecimal.ZERO);
            newLoan.setTimePeriod(genericService.getTimePeriod());
            newLoan.setSelectionScore(""); //Update in Second Thread for Loan Booking
            newLoan.setSelectionScoreRating(""); //Update in Second Thread for Loan Booking
            newLoan.setLimitRange("0");
            newLoan.setMsmeScore(String.valueOf("0"));
            newLoan.setPaystackRef(requestPayload.getPaystackRef());
            loanRepository.createLoan(newLoan);


            loanResponse.setResponseCode("00");
//            loanResponse.setResponseDescription("Your Loan Application is being processed, kindly check back in 30 mins to see if you have an offer");
            return "Your loan application has been received and currently in review. You will be notified as soon as possible";
        });

        //This thread completes the Loan booking
        CompletableFuture<String> completeLoanBooking = persistLoanRecordInitially.thenApply(result -> {
            Loan loanFailuresUpdate = loanRepository.getLoanUsingLoanId(loanId);
            log.info("Loan using loanUsingMobile  {}", gson.toJson("loanUsingMobile"));
            log.info("Loan Failure Update  {}", loanFailuresUpdate);

            IndicinaResponsePayload responsePayload;// = new IndicinaResponsePayload();
            MonoStatementResponsePayload monoResponsePayload;// = new MonoStatementResponsePayload();
            log.info("START SECOND THREAD HERE {}, ", "STARTING.........");

            //Log the request
            String requestJson = gson.toJson(requestPayload);
            genericService.generateLog("Digital Loan Booking", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
            try {
                //Connect to Mono to retrieve the account ID
                String monoRequestBody = "{\"code\":\"" + requestPayload.getMonoAccountCode() + "\"}";
                Unirest.setTimeouts(0, 0);
                HttpResponse<String> monoHttpResponse = Unirest.post(monoApiUrl + "/account/auth")
                        .header("mono-sec-key", monoSecretKey)
                        .header("Content-Type", "application/json")
                        .body(monoRequestBody)
                        .asString();

                monoResponsePayload = gson.fromJson(monoHttpResponse.getBody(), MonoStatementResponsePayload.class);

                //Check if the request failed
                if (monoResponsePayload.getId() == null) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking", token, monoResponsePayload.getMessage(), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity("", "Digital Loan Booking", requestPayload.getLoanAmount(), channel, monoResponsePayload.getMessage(), requestPayload.getMobileNumber(), 'F');
                    loanFailuresUpdate.setFailureReason(messageSource.getMessage("appMessages.loan.monoFailure", new Object[]{monoResponsePayload.getId()}, Locale.ENGLISH));
                    loanRepository.updateLoan(loanFailuresUpdate);
                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.monoFailure", new Object[]{monoResponsePayload.getId()}, Locale.ENGLISH));
                    return gson.toJson(errorResponse[0]);
                }

                //Get the Mono account id
                String accountId = monoResponsePayload.getId();
                MonoIdentityResponsePayload monoIdentityPayload = new MonoIdentityResponsePayload();
                //Fetch account details from Mono
                Unirest.setTimeouts(0, 0);
                HttpResponse<String> monoIdentityHttpResponse = Unirest.get(monoApiUrl + "/accounts/" + accountId + "/identity")
                        .header("mono-sec-key", monoSecretKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .asString();

                //Parse the response

                monoIdentityPayload = gson.fromJson(monoIdentityHttpResponse.getBody(), MonoIdentityResponsePayload.class);

                //Check if BVN from Mono is Empty
//                if (monoIdentityPayload.getBvn() == null) {
//                    //Log the error
//                    genericService.generateLog("Digital Loan Booking", token, "No BVN attached to record from Mono", "API Response", "INFO", requestPayload.getRequestId());
//                    //Create User Activity log
//                    genericService.createUserActivity("", "Digital Loan Booking", requestPayload.getLoanAmount(), channel, monoIdentityPayload.getBvn(), requestPayload.getMobileNumber(), 'F');
//                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
//                    errorResponse[0].setResponseMessage("appMessages.bvn.noBVN");
//                    return gson.toJson(errorResponse);
//                }

                String customerFullName = customer.getLastName() + " " + customer.getOtherName();
                // compare Mono BVN  or Name with Customer BVN or Name
                String[] monoFullNameSplit = monoIdentityPayload.getFullName().replace(",", " ").split(" ");
                String[] customerFullNameSplit = customerFullName.replace(",", " ").split(" ");
                boolean checkName = false;
                //Convert Customer name to array and put in a set
                Set<String> customerNameSet = new HashSet<>(Arrays.asList(customerFullNameSplit));
                for (String monoName : monoFullNameSplit) {
                    if (customerNameSet.contains(monoName)) {
                        checkName = true;
                        break;
                    }
                }
                log.info("Customer BVN {}", requestPayload.getBvn());
                boolean checkBvn = monoIdentityPayload.getBvn().equals(requestPayload.getBvn());
                if (!checkName || !checkBvn) {
                    genericService.generateLog("Digital Loan Booking", token, monoIdentityPayload.getBvn(), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    loanFailuresUpdate.setFailureReason("Customer BVN or Name Mismatch");
                    loanRepository.updateLoan(loanFailuresUpdate);
                    genericService.createUserActivity("", "Digital Loan Booking", requestPayload.getLoanAmount(), channel, monoIdentityPayload.getBvn(), requestPayload.getMobileNumber(), 'F');
                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse[0].setResponseMessage("appMessage.bvn.mismatchs");
                    return gson.toJson(errorResponse);
                }
//                loanFailuresUpdate.setFailureReason("Customer BVN or Name Mismatch");
//                log.info("Loan Failure guy {}", loanFailuresUpdate);
//                loanRepository.updateLoan(loanFailuresUpdate);
                //Fetch account statement from Mono
                Unirest.setTimeouts(0, 0);
                HttpResponse<String> monoStatementHttpResponse = Unirest.get(monoApiUrl + "/accounts/" + accountId + "/statement?last3Months")
                        .header("mono-sec-key", monoSecretKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .asString();

                //Parse the response
                monoResponsePayload = gson.fromJson(monoStatementHttpResponse.getBody(), MonoStatementResponsePayload.class);

                if (monoResponsePayload.getData() == null) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking", token, monoResponsePayload.getMessage(), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    loanFailuresUpdate.setFailureReason("Mono could not reach bank statement bank");
                    loanRepository.updateLoan(loanFailuresUpdate);
                    genericService.createUserActivity("", "Digital Loan Booking", requestPayload.getLoanAmount(), channel, monoResponsePayload.getMessage(), requestPayload.getMobileNumber(), 'F');
                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.monoFailure", new Object[]{monoResponsePayload.getId()}, Locale.ENGLISH));
                    return gson.toJson(errorResponse[0]);
                }


                //Generate Indicina token
                IndicinaRequestPayload tokenRequest = new IndicinaRequestPayload();
                tokenRequest.setClient_id(indicinaClientId);
                tokenRequest.setClient_secret(indicinaSeecretKey);
                String tokenRequestJson = gson.toJson(tokenRequest);
                Unirest.setTimeouts(0, 0);
                HttpResponse<String> indicinaTokenHttpResponse = Unirest.post(indicinaApiUrl + "/api/login")
                        .header("Content-Type", "application/json")
                        .body(tokenRequestJson)
                        .asString();

                responsePayload = gson.fromJson(indicinaTokenHttpResponse.getBody(), IndicinaResponsePayload.class);
                //Check if the request failed
                if (!"success".equalsIgnoreCase(responsePayload.getStatus())) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking", token, responsePayload.getMessage(), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity("", "Digital Loan Booking", requestPayload.getLoanAmount(), channel, responsePayload.getMessage(), requestPayload.getMobileNumber(), 'F');
                    loanFailuresUpdate.setFailureReason(responsePayload.getMessage());
                    loanRepository.updateLoan(loanFailuresUpdate);
                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse[0].setResponseMessage(responsePayload.getMessage());
                    return gson.toJson(errorResponse[0]);
                }
                String indicinaToken = responsePayload.getData().getToken();

                //Call the Indicina API for Account Statement Analysis
                List<Data> dataList = new ArrayList<>();
                for (Data d : monoResponsePayload.getData()) {
                    Data data = new Data();
                    data.setAmount(d.getAmount());
                    data.set_id(d.get_id());
                    data.setDate(d.getDate());
                    data.setNarration(d.getNarration());
                    data.setType(d.getType());
                    data.setCategory(d.getCategory());
                    dataList.add(data);
                }

                //Build the Paging
                Paging page = new Paging();
                page.setNext("https://api.withmono.com/accounts/:id/transactions?page=3");
                page.setPage(2);
                page.setPrevious("https://api.withmono.com/accounts/:id/transactions?page=2");
                page.setTotal(Integer.parseInt(monoResponsePayload.getMeta().getCount()));

                //Add to Content payload
                Content content = new Content();
                content.setData(dataList);
                content.setPaging(page);

                //Add to BankStatement payload
                BankStatement bankStatement = new BankStatement();
                bankStatement.setContent(content);
                bankStatement.setType("mono");

                CustomerPayload customerId = new CustomerPayload();
                customerId.setId(indicinaCustomerId);

                IndicinaRequestPayload indicinaRequest = new IndicinaRequestPayload();
                indicinaRequest.setAccount_id(accountId);
                indicinaRequest.setCustomer(customerId);
                indicinaRequest.setBankStatement(bankStatement);

                String indicinaStatementRequestJson = gson.toJson(indicinaRequest);
                genericService.generateLog("Indicina Statement Request", token, indicinaStatementRequestJson, "API Request", "INFO", requestPayload.getRequestId());

                Unirest.setTimeouts(0, 0);
                HttpResponse<String> indicinaStatementHttpResponse = Unirest.post(indicinaApiUrl + "/bsp")
                        .header("Authorization", "Bearer " + indicinaToken)
                        .header("Content-Type", "application/json")
                        .body(indicinaStatementRequestJson)
                        .asString();

                responsePayload = gson.fromJson(indicinaStatementHttpResponse.getBody(), IndicinaResponsePayload.class);
                genericService.generateLog("Indicina Stament Response", token, indicinaStatementHttpResponse.getBody(), "API RESPONSE", "INFO", requestPayload.getRequestId());

                genericService.generateLog("Indicina Status Response", token, responsePayload.getStatus(), "API Response", "INFO", requestPayload.getRequestId());

                //Check if the request failed
                if (!"success".equalsIgnoreCase(responsePayload.getStatus())) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking", token, responsePayload.getMessage(), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity("", "Digital Loan Booking", requestPayload.getLoanAmount(), channel, responsePayload.getMessage(), requestPayload.getMobileNumber(), 'F');
                    loanFailuresUpdate.setFailureReason("Failure processing bank statement");
                    loanRepository.updateLoan(loanFailuresUpdate);
                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    //  errorResponse.setResponseMessage(responsePayload.getMessage());
                    errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.indicinaFailure", new Object[]{responsePayload.getId()}, Locale.ENGLISH));
                    return gson.toJson(errorResponse[0]);
                }

                //Declare the weights
                double creditBureauWeight = 0, spendingPatternWeight = 0, liabilityWeight = 0, cashFlowWeight = 0;
                Map<String, Double> selectionScores = new HashMap<>();

                //Check the applied loan amount
                BigDecimal loanAmt = new BigDecimal(requestPayload.getLoanAmount());
                if (loanAmt.compareTo(loanType.getMinAmount()) < 0 || loanAmt.compareTo(loanType.getMaxAmount()) > 0) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.outofrange", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.outofrange", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                    newLoan.setFailureReason("Customer BVN or Name Mismatch");
                    loanRepository.updateLoan(newLoan);
                    errorResponse[0].setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                    errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.outofrange", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH));
                    return gson.toJson(errorResponse[0]);
                }

                //Call the BVN validation API
//            BVNRequestPayload bvnPayload = new BVNRequestPayload();
//            bvnPayload.setBvn(requestPayload.getBvn());
//            bvnPayload.setMobileNumber(requestPayload.getMobileNumber());
//            bvnPayload.setRequestId(genericService.generateRequestId());
//            bvnPayload.setToken(token);
//            bvnPayload.setHash(genericService.hashBVNValidationRequest(bvnPayload));
//            String bvnRequestPayload = gson.toJson(bvnPayload);
//
//            String bvnResponseJson = bvnService.bvnValidation(token, bvnRequestPayload);
//            BVNResponsePayload bvnResponsePayload = gson.fromJson(bvnResponseJson, BVNResponsePayload.class);
//            if (!bvnResponsePayload.getResponseCode().trim().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
//                //Log the error
//                genericService.generateLog("Digital Loan Booking", token, bvnResponseJson, "API Response", "INFO", requestPayload.getRequestId());
//                return bvnResponseJson;
//            }

                //Call the Credit Bureau API
                CreditBureauRequestPayload creditBureauPayload = new CreditBureauRequestPayload();
                creditBureauPayload.setMobileNumber(requestPayload.getMobileNumber());
                creditBureauPayload.setBvn(requestPayload.getBvn());
                creditBureauPayload.setSearchType("CREDITHISTORY");
                creditBureauPayload.setRequestId(requestPayload.getRequestId());
                creditBureauPayload.setToken(token);
                creditBureauPayload.setHash(genericService.hashCreditBureauValidationRequest(creditBureauPayload));
                String creditBureauRequestPayload = gson.toJson(creditBureauPayload);

                log.info("Call Credit Bureau {}", creditBureauService.creditBureauValidation(token, creditBureauRequestPayload));
                String creditBureauResponseJson = creditBureauService.creditBureauValidation(token, creditBureauRequestPayload);
                log.info("Call Credit Bureau RESPONSE JSON {}", creditBureauResponseJson);
                CreditBureauResponsePayload creditBureauResponsePayload = gson.fromJson(creditBureauResponseJson, CreditBureauResponsePayload.class);
                log.info("Call Credit Bureau RESPONSE PAYLOAD {}", creditBureauResponsePayload);
                genericService.generateLog("Digital Loan Booking: Credit Bureau Response", token, creditBureauResponseJson, "API Response", "INFO", requestPayload.getRequestId());

                if (!creditBureauResponsePayload.getResponseCode().trim().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking: Credit Bureau Response", token, creditBureauResponseJson, "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse[0] = gson.fromJson(creditBureauResponseJson, OmniResponsePayload.class);

                    //Check if it is not a 'NO HIT'
                    if (!errorResponse[0].getResponseCode().trim().equalsIgnoreCase(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode()) && !errorResponse[0].getResponseMessage().equalsIgnoreCase("NO HIT")) {
                        return creditBureauResponseJson;
                    }

                    //Its a no hit. Assign weight to it
                    creditBureauWeight = 0;
                    selectionScores.put("Credit Bureau", 0.0);
                }

                //Loop through the Credit Facility History
                List<CreditBureauPayload> creditHistory = creditBureauResponsePayload.getCreditFacilityHistory();
                if (creditHistory == null) {
                    creditBureauWeight = 0;
                    selectionScores.put("Credit Bureau", 0.0);
                } else {


                    // 1. Performing 2. Pass and Watch 3. Sub Standard 4. Doubtful 5. Lost
                    List<String> loanStatus = new ArrayList<>();
                    for (CreditBureauPayload ch : creditHistory) {
                        if (loanStatus.isEmpty()) {
                            break;
                        }
                        if (!loanStatus.contains(ch.getAssetClassification())) {
                            loanStatus.add(ch.getAssetClassification());
                        }

                    }
                    if (loanStatus.contains("Lost")) {
                        creditBureauWeight = -10;
                        selectionScores.put("Credit Bureau", -10.0);
                    } else if (!loanStatus.contains("Lost")) {
                        if (loanStatus.contains("Doubtful")) {
                            //Doubtful
                            creditBureauWeight = -5;
                            selectionScores.put("Credit Bureau", -5.0);
                        } else if (!loanStatus.contains("Doubtful")) {
                            if (loanStatus.contains("Sub Standard")) {
                                // Sub-Standard
                                creditBureauWeight = 5;
                                selectionScores.put("Credit Bureau", 5.0);
                            } else if (!loanStatus.contains("Sub Standard")) {
                                if (loanStatus.contains("Watch")) {
                                    //Pass and Watch
                                    creditBureauWeight = 7;
                                    selectionScores.put("Credit Bureau", 7.0);
                                } else if (!loanStatus.contains("Watch")) {
                                    if (loanStatus.contains("Performing")) {
                                        //Performing
                                        creditBureauWeight = 35;
                                        selectionScores.put("Credit Bureau", 35.0);
                                        genericService.generateLog("Loan Status in History", token, "Performing", "API Response", "INFO", requestPayload.getRequestId());

                                    }

                                }
                            }
                        }
                    }

                }

                genericService.generateLog("Checking Number of Inquiry", token, " ", " Credit Analysis API Response", "INFO", requestPayload.getRequestId());

                //Check the number of inquiry in the last 3 months
                int inquiry = genericService.getInquiryInLastThreeMonths(customer);
                genericService.generateLog("Maximum number of days: ", token, " ", Integer.toString(inquiry), "INFO", requestPayload.getRequestId());

                if (inquiry == 0) {
                    selectionScores.put("Inquiry In Last 3 Month", 1.0);
                } else if (inquiry > 0 && inquiry <= 10) {
                    selectionScores.put("Inquiry In Last 3 Month", 0.0);
                } else {
                    selectionScores.put("Inquiry In Last 3 Month", -2.0);
                }


                //Check the number of dishored cheques
                int dishonoredCheques = genericService.getDishonouredCheque(customer);
                genericService.generateLog("Checking Number of Dishonored Cheques", token, Integer.toString(dishonoredCheques), "API response", "INFO", requestPayload.getRequestId());

                if (dishonoredCheques == 0) {
                    selectionScores.put("Dishonored Cheques", 2.0);
                } else {
                    selectionScores.put("Dishonored Cheques", 0.0);
                }

                //Check the maximum overdue days
                int maxOverdueDays = genericService.getMaxOverdueDays(customer);
                genericService.generateLog("Maximum number of overdew days: ", token, Integer.toString(maxOverdueDays), "API response", "INFO", requestPayload.getRequestId());

                if (maxOverdueDays == 0) {
                    selectionScores.put("Maximum Days Overdue", 1.0);
                } else {
                    selectionScores.put("Maximum Days Overdue", -1.0);
                }

                //Check the number of suits and write off
                int writeOffs = genericService.getSuitAndWriteOff(customer);
                genericService.generateLog("writeOffs: ", token, Integer.toString(writeOffs), "API response", "INFO", requestPayload.getRequestId());

                if (writeOffs <= 0) {
                    selectionScores.put("Write Off", 2.0);
                } else {
                    selectionScores.put("Write Off", -2.0);
                }

                //Check for address match
//            boolean addressMatch = genericService.addressMatch(customer);
//            LOGGER.log(Level.INFO, "AddressMatch", addressMatch);
//
//            genericService.generateLog("Address Match: : ", token,  Boolean.toString(addressMatch),"API response", "INFO", requestPayload.getRequestId());
//
//            if (addressMatch) {
//                selectionScores.put("Address Match", 1.0);
//            } else {
//                selectionScores.put("Address Match", 0.0);
//            }

                //Check for Maximum Overdew Facility

                int overdiewFacility = genericService.getMaxOverdueFacility(customer);
                LOGGER.log(Level.INFO, "overdiewFacility", overdiewFacility);

                genericService.generateLog("Maximum Overdew  : ", token, " ", Integer.toString(overdiewFacility), "INFO", requestPayload.getRequestId());

                if (overdiewFacility == 0) {
                    selectionScores.put("Overdue Facility", 2.0);
                } else {
                    selectionScores.put("Overdue Facility", -1.0);
                }

                //Check the % of deliquent loans
                BigDecimal deliquentLoans = genericService.getDeliquentLoan(customer);
                LOGGER.log(Level.INFO, "Deliguent Loans", deliquentLoans);

                genericService.generateLog("deliquentLoans: ", token, " ", deliquentLoans.toString(), "INFO", requestPayload.getRequestId());

                if (deliquentLoans.compareTo(thresholdForDeliquentLoans) > 0) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.threshold.outofrange", new Object[]{"Deliquent Loan", thresholdForDeliquentLoans, deliquentLoans}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    loanFailuresUpdate.setFailureReason("The {0} value set to {1}. Credit Bureau value is {2}. Delinquent threshold high");
                    loanRepository.updateLoan(loanFailuresUpdate);
                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.threshold.outofrange", new Object[]{"Deliquent Loan", thresholdForDeliquentLoans, deliquentLoans}, Locale.ENGLISH));
                    return gson.toJson(errorResponse[0]);
                }

                //Check the % of maximum overdue amount
                BigDecimal maxOverdueAmount = genericService.getMaxOverdueAmount(customer);
                LOGGER.log(Level.INFO, "maxOverdueAmount", maxOverdueAmount);

                genericService.generateLog("maxOverdueAmount: ", token, " ", maxOverdueAmount.toString(), "INFO", requestPayload.getRequestId());

                if (maxOverdueAmount.compareTo(thresholdForOverdueAmount) > 0) {
                    //Log the error
                    genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.threshold.outofrange", new Object[]{"Maximum Overdue Amount", thresholdForOverdueAmount, maxOverdueAmount}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    loanFailuresUpdate.setFailureReason("The {0} value set to {1}. Credit Bureau value is {2}. Delinquent threshold high");
                    loanRepository.updateLoan(loanFailuresUpdate);
                    errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.threshold.outofrange", new Object[]{"Maximum Overdue Amount", thresholdForOverdueAmount, maxOverdueAmount}, Locale.ENGLISH));
                    return gson.toJson(errorResponse[0]);
                }

                //Analyze the Indicina Account Statement response
                Double otherIncome = 0.0;
                try {
                    otherIncome = Double.valueOf(responsePayload.getData().getIncomeAnalysis().getAverageOtherIncome());
                } catch (NumberFormatException numberFormatException) {

                }

                Double salaryIncome = 0.0;
                try {
                    salaryIncome = Double.valueOf(responsePayload.getData().getIncomeAnalysis().getAverageSalary());
                } catch (NumberFormatException numberFormatException) {
                }
                Double totalIncome = otherIncome + salaryIncome;
                selectionScores.put("Income", totalIncome);
                Double availableIncome = 0.7 * totalIncome;
                Double loanableAmount = DEBT_SERVICE_RATIO * totalIncome;
                selectionScores.put("Approved Amount", loanableAmount);

                //Get the Liability
                Double totalExpense = 0.0;
                try {
                    totalExpense = Double.valueOf(responsePayload.getData().getSpendAnalysis().getTotalExpenses());
                } catch (NumberFormatException | NullPointerException ex) {
                }
                // it skips the process at this line 624
                Double totalExpenseOnTransfer = 0.0;
                try {
                    totalExpenseOnTransfer = Double.valueOf(responsePayload.getData().getSpendAnalysis().getSpendOnTransfers());
                } catch (NumberFormatException | NullPointerException ex) {
                }
                //Double totalExpenseOnTransfer = 10000.0;
                Double totalLiability = totalExpense + totalExpenseOnTransfer;
                genericService.generateLog("totalLiability: ", token, " ", totalLiability.toString(), "INFO", requestPayload.getRequestId());

                if (totalLiability <= availableIncome) {
                    //Low Risk
                    liabilityWeight = 35;
                    selectionScores.put("Liability", 35.0);
                } else if (totalLiability > availableIncome) {
                    //High Risk
                    liabilityWeight = 5;
                    selectionScores.put("Liability", 5.0);
                }

                //Check the Spending Pattern. Loop through all the expenses
                Double spendTotal = Double.valueOf(0);

                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getAirtime());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }

                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getAtmWithdrawalsSpend());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getAverageRecurringExpense());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getBankCharges());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getBills());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getCableTv());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getClubsAndBars());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getGambling());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getReligiousGiving());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getUtilitiesAndInternet());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getWebSpend());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                try {
                    spendTotal += Double.valueOf(responsePayload.getData().getSpendAnalysis().getUssdTransactions());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }
                genericService.generateLog("spendTotal: ", token, " ", spendTotal.toString(), "INFO", requestPayload.getRequestId());

                if (spendTotal < (0.5 * totalIncome)) {
                    //Spending pattern is satisfactory
                    spendingPatternWeight = 10;
                    selectionScores.put("Spending Pattern", 10.0);
                } else if (spendTotal >= (0.5 * totalIncome) && spendTotal <= (0.8 * totalIncome)) {
                    //Spending pattern is Average
                    spendingPatternWeight = 6;
                    selectionScores.put("Spending Pattern", 6.0);
                } else {
                    //Spending pattern is Unsatisfactory
                    spendingPatternWeight = 1;
                    selectionScores.put("Spending Pattern", 1.0);
                }

                //Check the Cash Flow Analysis
                Double totalDebitTurnover = null;
                try {
                    totalDebitTurnover = Double.valueOf(responsePayload.getData().getCashFlowAnalysis().getTotalDebitTurnover());
                } catch (NumberFormatException | NullPointerException numberFormatException) {
                }

                try {
                    Double totalCreditTurnover = Double.valueOf(responsePayload.getData().getCashFlowAnalysis().getTotalCreditTurnover());

                    Double totalTurnover = totalCreditTurnover - totalDebitTurnover;

                    if (totalTurnover <= 0.2 * totalIncome) {
                        //Spend analysis is Unsatisfactory
                        cashFlowWeight = 2;
                        selectionScores.put("Cash Flow", 2.0);
                    } else if ((totalTurnover > 0.2 * totalIncome) && (totalTurnover <= 0.35 * totalIncome)) {
                        //Spend analysis is Average
                        cashFlowWeight = 6;
                        selectionScores.put("Cash Flow", 6.0);
                    } else {
                        //Spend analysis is Satisfactory
                        cashFlowWeight = 10;
                        selectionScores.put("Cash Flow", 10.0);
                    }

                    //Get the total weight
                    Double totalWeight = cashFlowWeight + spendingPatternWeight + liabilityWeight + creditBureauWeight;
                    Double approvedLoanAmount = (double) 0;
                    String scoreRating = "";
                    boolean loanRequestOk = true;
                    if (totalWeight >= 80 && totalWeight <= 100) {
                        //User gets 100% of the loanable amount
                        approvedLoanAmount = loanableAmount;
                        scoreRating = "A";
                    } else if (totalWeight >= 60 && totalWeight <= 79) {
                        //User gets 80% of the loanable amount
                        approvedLoanAmount = 0.8 * loanableAmount;
                        scoreRating = "B";
                        //     } else if (totalWeight >= 40 && totalWeight <= 59) {
                        // to be removed in production
                    } else if (totalWeight >= 20 && totalWeight <= 59) {
                        //User gets 50% of the loanable amount
                        approvedLoanAmount = 0.5 * loanableAmount;
                        scoreRating = "C";
                    } else {
                        loanRequestOk = false;
                    }

                    //Round up the approved amount
                    double tempAmount = approvedLoanAmount / 1000;
                    double roundupAmount = (double) Math.ceil(tempAmount);
                    approvedLoanAmount = roundupAmount * 1000;
                    genericService.generateLog("Approved  approvedLoanAmount : ", token, Double.toString(approvedLoanAmount), "API RESPONSE", "INFO", requestPayload.getRequestId());
                    if (approvedLoanAmount == 0) {

                        genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.threshold.outofrange", new Object[]{"Maximum Overdue Amount", thresholdForOverdueAmount, maxOverdueAmount}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                        loanFailuresUpdate.setFailureReason("Sorry, you do not qualify for a digital loan, Approved Loan Amount is 0");
                        loanRepository.updateLoan(loanFailuresUpdate);
                        errorResponse[0].setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                        errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.application.failed", new Object[]{"Zero loan amount", "", ""}, Locale.ENGLISH));
                        return gson.toJson(errorResponse[0]);

                    }
                    if (customer.getKycTier().equalsIgnoreCase("2") && approvedLoanAmount > 100000.00) {
                        approvedLoanAmount = 100000.00;
                    }

                    //Update the loan application to the DB
                    Loan updateLoan = loanRepository.getLoanUsingLoanId(loanId);

                    updateLoan.setFirstRepaymentDate(LocalDate.now());
                    updateLoan.setLoanAmountApproved(new BigDecimal(approvedLoanAmount));
                    updateLoan.setStatus(loanRequestOk ? "PENDING" : "DECLINED");
                    updateLoan.setSelectionScore(String.valueOf(totalWeight));
                    updateLoan.setSelectionScoreRating(scoreRating);

                    loanRepository.updateLoan(updateLoan);

                    //Persist the selection scores
                    selectionScores.forEach((k, v) -> {
                        SelectionScore newSC = new SelectionScore();
                        newSC.setCreatedAt(LocalDateTime.now());
                        newSC.setCriterion(k);
                        newSC.setLoanId(newLoan.getId());
                        newSC.setScore(v.toString());
                        newSC.setSource("ACCION");
                        loanRepository.createSelectionScore(newSC);
                    });

                    //Check if the loan request is not okay. Decline
                    if (!loanRequestOk) {
                        //Log the error
                        genericService.generateLog("Digital Loan Booking", token, messageSource.getMessage("appMessages.loan.declined", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                        //Create User Activity log
                        genericService.createUserActivity(customerNumber, "Digital Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.declined", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                        loanFailuresUpdate.setFailureReason("Loan Offer Declined");
                        loanRepository.updateLoan(loanFailuresUpdate);
                        errorResponse[0].setResponseCode(ResponseCodes.LOAN_DECLINED.getResponseCode());
                        errorResponse[0].setResponseMessage(messageSource.getMessage("appMessages.loan.declined", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH));
                        return gson.toJson(errorResponse[0]);
                    }

                    List<LoanOfferResponsePayload> loanOffers = new ArrayList<>();

                    double baseInterestRate = loanType.getInterestRate();
                    //Build a 4 months offer
//                for (int i = 1; i <= 4; i++) {
//                    //Generate monthly repayment
//                    String monthlyRepayment = genericService.loanRepayment(
//                            new BigDecimal(approvedLoanAmount),
//                            new BigDecimal(baseInterestRate * i), i);
//
//                    LoanOfferResponsePayload monthlyOffer = new LoanOfferResponsePayload();
//                    monthlyOffer.setApprovedAmount(String.valueOf(approvedLoanAmount));
//                    monthlyOffer.setMonthlyRepayment(monthlyRepayment);
//                    monthlyOffer.setRate(String.valueOf(baseInterestRate) + "% monthly");
//                    monthlyOffer.setTenor(String.valueOf(i) + "Month(s)");
//                    monthlyOffer.setTotalRepayment(String.valueOf(Double.valueOf(monthlyRepayment) * i));
//                    String loanOptionId = genericService.generateRequestId();
//                    monthlyOffer.setOptionId(loanOptionId);
//                    loanOffers.add(monthlyOffer);
//
//                    //Persist the loan options
//                    LoanOptions loanOptions = new LoanOptions();
//                    loanOptions.setApprovedAmount(String.valueOf(approvedLoanAmount));
//                    loanOptions.setCreatedAt(LocalDateTime.now());
//                    loanOptions.setInterestRate(String.valueOf(baseInterestRate));
//                    loanOptions.setLoanId(loanId);
//                    loanOptions.setMonthlyRepayment(monthlyRepayment);
//                    loanOptions.setTenor(String.valueOf(i));
//                    loanOptions.setTotalRepayment(String.valueOf(Double.valueOf(monthlyRepayment) * i));
//                    loanOptions.setLoanOptionId(loanOptionId);
//                    loanRepository.createLoanOptions(loanOptions);
//
//                    //Add 2% to the base interest rate for each month
//                    baseInterestRate += 2;
//                }

//                double totalInterest = 0;
//                double loanAmount = approvedLoanAmount;
//                double monthlyDeduction = 0;
//
//
//                for (int i = 1; i <= 4; i++) {
//                    //Generate monthly repayment
//                    double interest = 0.0;
//                    String monthlyRepayment = genericService.loanRepayment(
//                            new BigDecimal(approvedLoanAmount),
//                            new BigDecimal(baseInterestRate * i), i);
//                    double monthlyDeduction = approvedLoanAmount / i;
//                    LoanRepaymentResponsePayload oLoanRepaymentResponsePayload = new LoanRepaymentResponsePayload();
//                    for (int j = 1; j <= i; j++) {
//                        interest = baseInterestRate / 100 * approvedLoanAmount;
//                        approvedLoanAmount = approvedLoanAmount - monthlyDeduction;
//                        double duePayment = monthlyDeduction + interest;
//                        oLoanRepaymentResponsePayload.setRepayment(String.valueOf(duePayment));
//                    }
//                    loanRepayment.add(oLoanRepaymentResponsePayload);
//
//
//                    LoanOfferResponsePayload monthlyOffer = new LoanOfferResponsePayload();
//                    monthlyOffer.setApprovedAmount(String.valueOf(approvedLoanAmount));
//                    monthlyOffer.setMonthlyRepayment(monthlyRepayment);
//                    monthlyOffer.setRate(String.valueOf(baseInterestRate) + "% monthly");
//                    monthlyOffer.setTenor(String.valueOf(i) + "Month(s)");
//                    monthlyOffer.setTotalRepayment(String.valueOf(Double.valueOf(monthlyRepayment) * i));
//                    monthlyOffer.setRepayment(loanRepayment.toString());
//                    String loanOptionId = genericService.generateRequestId();
//                    monthlyOffer.setOptionId(loanOptionId);
//                    loanOffers.add(monthlyOffer);
//
//                    //Persist the loan options
//                    LoanOptions loanOptions = new LoanOptions();
//                    loanOptions.setApprovedAmount(String.valueOf(approvedLoanAmount));
//                    loanOptions.setCreatedAt(LocalDateTime.now());
//                    loanOptions.setInterestRate(String.valueOf(baseInterestRate));
//                    loanOptions.setLoanId(loanId);
//                    loanOptions.setMonthlyRepayment(monthlyRepayment);
//                    loanOptions.setTenor(String.valueOf(i));
//                    loanOptions.setTotalRepayment(String.valueOf(Double.valueOf(monthlyRepayment) * i));
//                    loanOptions.setLoanOptionId(loanOptionId);
//                    loanOptions.setRepayment(loanRepayment.toString());
//                    loanRepository.createLoanOptions(loanOptions);
//
//                    //Add 2% to the base interest rate for each month
//                    baseInterestRate += 2;
//                }
                    List<String> monthlyRepaymentList = new ArrayList<>();
                    for (int i = 1; i <= 4; i++) {
                        //Generate monthly repayment

                        String monthlyRepayment = genericService.loanRepaymentFlatRate(
                                new BigDecimal(approvedLoanAmount),
                                new BigDecimal(baseInterestRate), i);
                        ArrayList<String> monthlyRepayList = genericService.loanRepaymentFlatRateList(
                                new BigDecimal(approvedLoanAmount),
                                new BigDecimal(baseInterestRate), i);

                        log.info("Base Interest Rate {}", baseInterestRate);

                        monthlyRepaymentList.add(monthlyRepayment);
                        LoanOfferResponsePayload monthlyOffer = new LoanOfferResponsePayload();
                        monthlyOffer.setApprovedAmount(String.valueOf(approvedLoanAmount));
                        monthlyOffer.setMonthlyRepayment(monthlyRepayment);
                        monthlyOffer.setRate(baseInterestRate + "% monthly");
                        monthlyOffer.setTenor(i + "Month(s)");
                        String repayment = String.valueOf(Double.parseDouble(monthlyRepayment) * i);
                        monthlyOffer.setTotalRepayment(repayment);
                        monthlyOffer.setRepayments(monthlyRepayList);
                        String loanOptionId = genericService.generateRequestId();
                        monthlyOffer.setOptionId(loanOptionId);
                        loanOffers.add(monthlyOffer);

                        //Persist the loan options
                        LoanOptions loanOptions = new LoanOptions();
                        loanOptions.setApprovedAmount(String.valueOf(approvedLoanAmount));
                        loanOptions.setCreatedAt(LocalDateTime.now());
                        loanOptions.setInterestRate(String.valueOf(baseInterestRate));
                        loanOptions.setLoanId(loanId);
                        loanOptions.setMonthlyRepayment(monthlyRepayment);
                        loanOptions.setTenor(String.valueOf(i));
                        loanOptions.setTotalRepayment(repayment);
                        loanOptions.setLoanOptionId(loanOptionId);
                        String monthlyRepaymentJson = gson.toJson(monthlyRepayList);
                        loanOptions.setRepayment(String.valueOf(monthlyRepayList));
                        loanRepository.createLoanOptions(loanOptions);

                        //Add 2% to the base interest rate for each month
                        baseInterestRate += 2;
                    }
                    //Log the error.
                    genericService.generateLog("Digital Loan Booking", token, response, "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(disbursementAccount.getAccountNumber(), "Digital Loan Booking", requestPayload.getLoanAmount(), channel, "Success", requestPayload.getMobileNumber(), 'S');

                    loanResponse.setCustomerName(customer.getLastName() + " " + customer.getOtherName());
                    loanResponse.setFirstRepaymentDate(LocalDate.now().toString());
                    loanResponse.setLoanAmountRequested(genericService.formatAmountWithComma(requestPayload.getLoanAmount()));
                    loanResponse.setLoanId(loanId);
                    loanResponse.setLoanType(loanType.getLoanDescription());
                    loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    loanResponse.setLoanOffers(loanOffers);
                    loanResponse.setResponseDescription("Loan Booking Completed");
                    genericService.generateLog("Loan Response : ", token, gson.toJson(loanResponse), "API RESPONSE", "INFO", requestPayload.getRequestId());

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return gson.toJson(loanResponse);

                } catch (Exception ex) {
                    genericService.generateLog("Digital Loan Booking", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                    errorResponse[0].setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                    errorResponse[0].setResponseMessage(ex.getMessage());
                    return gson.toJson(errorResponse[0]);
                }
            } catch (Exception ex) {
                //Log the response
                genericService.generateLog("Digital Loan Booking", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
                loanFailuresUpdate.setFailureReason("Internal Server Error");
                loanRepository.updateLoan(loanFailuresUpdate);
                errorResponse[0].setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                errorResponse[0].setResponseMessage(ex.getMessage());
                return gson.toJson(errorResponse[0]);
            }

        });

        //This thread sends SMS to customer for Loan Offer and sets paystack reference in the Loan Record
        CompletableFuture<Void> sendSms = completeLoanBooking.thenRun(() -> {


            // Call Loan table again after updating to get updated status
            Loan loanUsingLoanId = loanRepository.getLoanUsingLoanId(loanId);

            //Set Email Payload
            NotificationPayload emailPayload = new NotificationPayload();
            emailPayload.setMobileNumber(requestPayload.getMobileNumber());
            emailPayload.setRecipientName(customer.getLastName() + ", " + customer.getOtherName());
//            emailPayload.setRecipientEmail(customer.getEmail());
            emailPayload.setRequestId(requestPayload.getRequestId());
            //If no error in Loan Booking
            if (loanUsingLoanId.getStatus().equalsIgnoreCase("PENDING")) {
                assert loanRecord != null;
                emailPayload.setDisbursementAccount(loanUsingLoanId.getDisbursementAccount());
                emailPayload.setEmailSubject("PENDING LOAN OFFER FOR " + customer.getLastName() + ", " + customer.getOtherName());
                emailPayload.setFailureReason("N/a");
//                emailPayload.setLoanDisbursementId(loanUsingLoanId.getLoanDisbursementId());
                emailPayload.setLoanAmount(String.valueOf(loanUsingLoanId.getLoanAmountApproved()));
                emailPayload.setStatus("PENDING OFFER");
            } else {
                emailPayload.setDisbursementAccount("N/a");
                emailPayload.setStatus("DECLINED OFFER");
//                emailPayload.setLoanDisbursementId("N/a");
                emailPayload.setLoanAmount("00");
                emailPayload.setEmailSubject("DECLINED LOAN OFFER FOR " + customer.getLastName() + ", " + customer.getOtherName());
                emailPayload.setFailureReason(loanUsingLoanId.getFailureReason());
            }
            emailPayload.setToken(token);
            genericService.sendLoanEmail(emailPayload);

            //Create SMS Payload
            SMS newSMS = new SMS();
            newSMS.setAppUser(loanUsingLoanId.getAppUser());
            newSMS.setCreatedAt(LocalDateTime.now());
            newSMS.setFailureReason("");
            if (loanUsingLoanId.getStatus().equalsIgnoreCase("PENDING")) {
                newSMS.setMessage("Congratulations! Your SaveBrighta loan application with ACCION MFB has been approved." +
                        "Accion Microfinance Bank");
            } else {
                newSMS.setMessage("We regret to inform you that you Loan Application was declined!" +
                        "Accion Microfinance Bank");
            }

            newSMS.setMobileNumber(loanUsingLoanId.getMobileNumber()); //Update properly after
            newSMS.setRequestId(loanUsingLoanId.getRequestId());
            newSMS.setSmsFor("Loan Booking");
            newSMS.setSmsType('N');
            newSMS.setStatus("PENDING");
            newSMS.setTimePeriod(genericService.getTimePeriod());
//                    newSMS.setLoan(loanUsingLoanId);
            log.info("Send SMS Payload Request {}", newSMS);
            SMSResponsePayload sendSmsResponse = digitalService.sendSms(newSMS);
            log.info("Send SMS Response {}", sendSmsResponse);

            //Call Paystack to get Transaction Details

            PaystackTransactionDetailsResponsePayload paystackDetails = null;
            try {
                paystackDetails = digitalService.processPaystackCardTransactionDetails(loanUsingLoanId.getPaystackRef(), loanId);
            } catch (UnirestException e) {
                throw new RuntimeException(e);
            }

            log.info("PAYSTACK DETAILS RESPONSE{}", paystackDetails);

        });

        loanResponse.setResponseCode("00");
        loanResponse.setResponseDescription(persistLoanRecordInitially.join());
        return gson.toJson(loanResponse);
    }

    @SuppressWarnings("unused")
    public String digitalLoanBookingFallback(String token, DigitalLoanRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateDigitalLoanAcceptancePayload(String token, DigitalLoanAcceptanceRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getLoanOptionId().trim());
        rawString.add(requestPayload.getRequestId().trim());
//        rawString.add(requestPayload.getPin().trim());
//        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        String decryptedString = genericService.decryptString("requestPayload.getHash()", encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "digitalLoanAcceptanceFallback")
    public String processDigitalLoanAcceptance(String token, DigitalLoanAcceptanceRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Digital Loan Acceptance", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        LoanResponsePayload loanResponse = new LoanResponsePayload();
        try {
            //Check the loan option selected
            LoanOptions loanOptions = loanRepository.getLoanOptionsUsingId(requestPayload.getLoanOptionId(), requestPayload.getLoanId());
            if (loanOptions == null) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.loan.option.notexist", new Object[]{requestPayload.getLoanOptionId(), requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Digital Loan Acceptance", "", channel, messageSource.getMessage("appMessages.loan.option.notexist", new Object[]{requestPayload.getLoanOptionId(), requestPayload.getLoanId()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.option.notexist", new Object[]{requestPayload.getLoanOptionId(), requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Digital Loan Acceptance", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the customer number
            String customerNumber = customer.getCustomerNumber().trim();
            //Check the status of the customer
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Acceptance", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //validate IMEI FOR MOBILE CHANNEL
//            if ("MOBILE".equalsIgnoreCase(channel)) {
////                boolean imeiMatch = bCryptEncoder.matches(requestPayload.getImei(), customer.getImei());
//                if (true) {
//                    genericService.generateLog("Customer Details", token, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                    genericService.createUserActivity("", "Validate IMEI", "appMessages.customer.imei.invalid", channel, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[0], Locale.ENGLISH), requestBy, 'F');
//                    errorResponse.setResponseCode(ResponseCodes.IMEI_MISMATCH.getResponseCode());
//                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH));
//                    return gson.toJson(errorResponse);
//                }
//            }
            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Acceptance", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.loan.record.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the status of the loan request
//            if (!"PENDING".equalsIgnoreCase(loanRecord.getStatus())) {
//                //Log the error
//                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                genericService.createUserActivity(customerNumber, "Digital Loan Acceptance", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
//                return gson.toJson(errorResponse);
//            }

            //Check if the loan is a digital loan
            if (!LoanCategory.BRIGHTA_LOAN.getCategoryCode().equalsIgnoreCase(loanRecord.getLoanSetup().getLoanCategory())) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Acceptance", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Digital Loan Acceptance", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Validate the PIN
//            boolean pinMatch = bCryptEncoder.matches(requestPayload.getPin(), customer.getPin());
//            if (!pinMatch) {
//                //Log the error
//                genericService.generateLog("Password Update", token, messageSource.getMessage("appMessages.mismatch.pin", new Object[]{"PIN", "Mobile ", requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                genericService.createUserActivity(requestPayload.getRequestId(), "Password Update", "", channel, messageSource.getMessage("appMessages.mismatch.pin", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.PASSWORD_PIN_MISMATCH.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.mismatch.pin", new Object[]{"PIN", "Mobile ", requestPayload.getMobileNumber()}, Locale.ENGLISH));
//                return gson.toJson(errorResponse);
//            }


            loanRecord.setStatus("ACCEPTED");
            loanRecord.setLoanAmountApproved(new BigDecimal(loanOptions.getApprovedAmount()));
            loanRecord.setLoanTenor(loanOptions.getTenor());
            loanRecord.setMonthlyRepayment(new BigDecimal(loanOptions.getMonthlyRepayment()));
            loanRecord.setFirstRepaymentDate(LocalDate.now().plusDays(30));
            loanRecord.setTotalRepayment(new BigDecimal(loanOptions.getTotalRepayment()));
            loanRecord.setInterestRate(new BigDecimal(loanOptions.getInterestRate()));
            loanRepository.updateLoan(loanRecord);

            loanResponse.setCustomerName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
            loanResponse.setFirstRepaymentDate(LocalDate.now().toString());
            loanResponse.setInterestRate(loanRecord.getLoanSetup().getInterestRate() + "%");
            loanResponse.setLoanAmountRequested(loanRecord.getLoanAmountRequested().toString());
            loanResponse.setLoanAmountApproved(loanRecord.getLoanAmountApproved().toString());
            loanResponse.setLoanId(loanRecord.getLoanId());
            loanResponse.setLoanDisbursementId(loanRecord.getLoanDisbursementId());
            loanResponse.setLoanTenor(loanRecord.getLoanTenor());
            loanResponse.setLoanType(loanRecord.getLoanSetup().getLoanName());
            loanResponse.setMonthlyRepayment(loanRecord.getMonthlyRepayment().toString());
            loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
//            return gson.toJson(loanResponse);

            //Begin loan disbursement process
            DisburseLoanResponsePayload disburseLoanResponsePayload; // = new DisburseLoanResponsePayload();
            try {
                LoanIdRequestPayload loanIdRequestPayload = new LoanIdRequestPayload();
                loanIdRequestPayload.setLoanId(loanRecord.getLoanId());
                loanIdRequestPayload.setRequestId(loanRecord.getId().toString());

                String loanDisbursementResponseJson = digitalService.processDigitalLoanDisbursement(token, loanIdRequestPayload);
                disburseLoanResponsePayload = gson.fromJson(loanDisbursementResponseJson, DisburseLoanResponsePayload.class);

                boolean disbursed = disburseLoanResponsePayload.getResponseCode().trim().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode());
                if (disbursed) {
                    Loan loanDisbursed = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
                    //Create Schedule after disbursing loan
                    digitalService.processSchedule(token, requestPayload.getMobileNumber(), requestPayload.getLoanOptionId());
                    //Place lien on Loan Amount after successful disbursement
                    LockAmountDto lockAmount = new LockAmountDto();
                    lockAmount.setStartDate(LocalDate.now());
                    lockAmount.setEndDate(LocalDate.now().plusYears(10));
                    lockAmount.setAmount(loanOptions.getApprovedAmount());
                    lockAmount.setRequestId(genericService.generateRequestId());
                    lockAmount.setLoanId(loanDisbursed.getLoanId());
                    lockAmount.setAccountNumber(loanDisbursed.getDisbursementAccount());
                    digitalService.processPlaceLien(token, lockAmount);


                    //Send Email after placing Lien
                    NotificationPayload emailPayload = new NotificationPayload();
                    emailPayload.setMobileNumber(requestPayload.getMobileNumber());
                    emailPayload.setRecipientName(customer.getLastName() + ", " + customer.getOtherName());
                    emailPayload.setRequestId(requestPayload.getRequestId());
                    emailPayload.setDisbursementAccount(loanRecord.getDisbursementAccount());
                    emailPayload.setEmailSubject("Digital Loan Alert -  " + " " + loanRecord.getLoanDisbursementId() + " - " + customer.getLastName() + ", " + customer.getOtherName() + "/" + loanRecord.getLoanAmountApproved());
                    emailPayload.setFailureReason("N/a");
                    emailPayload.setLoanDisbursementId(loanDisbursed.getLoanDisbursementId());
                    emailPayload.setLoanAmount(String.valueOf(loanDisbursed.getLoanAmountApproved()));
                    emailPayload.setStatus("DISBURSED");

                    emailPayload.setToken(token);
                    genericService.sendLoanEmail(emailPayload);

                    return gson.toJson(loanResponse);
                } else {
                    //disbuursement data to build this response
                    //loanResponse  
                    return loanDisbursementResponseJson;
                }
            } catch (Exception ex) {
                genericService.generateLog("Digital Loan Disbursement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                return (ex.getMessage());

            }

        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Digital Loan Acceptance", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }

        //  return gson.toJson(loanResponse);
    }

    @Override
    public ScheduleResponsePayload processSchedule(String token, String mobileNumber, String loanOptionsId) {
        ScheduleResponsePayload errorResponse = new ScheduleResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        //Log the request

        genericService.generateLog("Digital Loan Schedule", token, mobileNumber, "API Request", "INFO", loanOptionsId);

        Schedule schedule = new Schedule();
        ScheduleResponsePayload scheduleResponse = new ScheduleResponsePayload();
        ScheduleResponsePayload scheduleResponsePayload = new ScheduleResponsePayload();
        try {
            Loan loanRecord = loanRepository.getLoanUsingMobileNumber(mobileNumber);
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.loan.noexist", new Object[]{0}, Locale.ENGLISH), "API Response", "INFO", loanOptionsId);
                //Create User Activity log
                genericService.createUserActivity(mobileNumber, "Digital Loan Schedule", loanOptionsId, channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), mobileNumber, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.noexist", new Object[]{requestBy}, Locale.ENGLISH));
                return errorResponse;
            }

            LoanOptions loanOptions = loanRepository.getLoanOptionsUsingId(loanOptionsId, loanRecord.getLoanId());
            if (loanOptions == null) {
                //Log the error
                genericService.generateLog("Digital Loan Acceptance", token, messageSource.getMessage("appMessages.loan.option.notexist", new Object[]{mobileNumber}, Locale.ENGLISH), "API Response", "INFO", loanOptionsId);
                //Create User Activity log
                genericService.createUserActivity(mobileNumber, "Digital Loan Schedule", loanOptionsId, channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), loanOptionsId, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loanOptions.noexist", new Object[]{requestBy}, Locale.ENGLISH));
                return errorResponse;
            }
            String repayments = loanOptions.getRepayment();
            String[] repaymentStringList = repayments.split(",");
            List<String> repaymentList = new ArrayList<>();
            //Persist in the Schedule Table
            int counter = 1;
            LocalDate dueMonth = loanRecord.getDisbursedAt().plusDays(30);
            double outstandingBalance = 0.00;
            for (String amount : repaymentStringList) {
                schedule = new Schedule();
                repaymentList.add(amount);
                schedule.setLoanId(loanRecord.getLoanId());
                schedule.setLoanOptionsId(loanOptions.getLoanOptionId());
                schedule.setDisbursedDate(loanRecord.getDisbursedAt());
                schedule.setRepaymentDate(dueMonth);
                dueMonth = dueMonth.plusDays(30);
                schedule.setMonths(String.valueOf(counter));
                schedule.setLoanDisbursementId(loanRecord.getLoanDisbursementId());
                schedule.setDisbursementAccount(loanRecord.getDisbursementAccount());
                counter++;
                double balance;
                String dueAmount = schedule.getPastDueAmount();
                balance = (Double.parseDouble(loanOptions.getMonthlyRepayment()) + Double.parseDouble(dueAmount)) - Double.parseDouble(schedule.getEarlyRepaymentAmount());
                DecimalFormat decimalFormat = new DecimalFormat("0.00");
                double balanceFormat = Double.parseDouble(decimalFormat.format(balance));
                outstandingBalance = balanceFormat + outstandingBalance;
                schedule.setOutstandingBalance(String.valueOf(outstandingBalance));
                schedule.setRepaymentAmount(String.valueOf(balanceFormat));
                schedule.setLoanBalance(loanOptions.getMonthlyRepayment());
                String amounts = repaymentList.toString();
                String amountSplit = amounts.replace("[", "").replace("]", "");
                schedule.setPaystackRef(loanRecord.getPaystackRef());
                loanRepository.createSchedule(schedule);
            }
            log.info("OUTSTANDING BALANCE {}", outstandingBalance);
            scheduleResponsePayload = new ScheduleResponsePayload();
            scheduleResponsePayload.setResponseCode("00");
            scheduleResponsePayload.setResponseMessage("Schedule created");
            scheduleResponsePayload.setLoanOptionsId(schedule.getLoanOptionsId());
            scheduleResponsePayload.setRepaymentAmount(schedule.getRepaymentAmount());
            scheduleResponsePayload.setDueDate(String.valueOf(schedule.getRepaymentDate()));
            scheduleResponsePayload.setEarlyRepaymentAmount(schedule.getEarlyRepaymentAmount());
            scheduleResponsePayload.setPastDueAmount(schedule.getPastDueAmount());
            scheduleResponsePayload.setLoanId(schedule.getLoanId());
            scheduleResponsePayload.setDisbursementAccount(schedule.getDisbursementAccount());
            scheduleResponsePayload.setPaystackRef(schedule.getPaystackRef());
            scheduleResponsePayload.setLoanId(scheduleResponse.getLoanId());


        } catch (Exception e) {
            e.getMessage();
        }
        return scheduleResponsePayload;
    }

    @SuppressWarnings("unused")
    public String digitalLoanAcceptanceFallback(String token, DigitalLoanAcceptanceRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateDigitalLoanDeclinePayload(String token, DigitalLoanDeclineRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getLoanOptionId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "digitalLoanDeclineFallback")
    public String processDigitalLoanDecline(String token, DigitalLoanDeclineRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Digital Loan Decline", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            // Check the loan option selected
            LoanOptions loanOptions = loanRepository.getLoanOptionsUsingId(requestPayload.getLoanOptionId(), requestPayload.getLoanId());
            if (loanOptions == null) {
                //Log the error
                genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.loan.option.notexist", new Object[]{requestPayload.getLoanOptionId(), requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Digital Loan Decline", "", channel, messageSource.getMessage("appMessages.loan.option.notexist", new Object[]{requestPayload.getLoanOptionId(), requestPayload.getLoanId()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.option.notexist", new Object[]{requestPayload.getLoanOptionId(), requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Digital Loan Decline", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the customer number
            String customerNumber = customer.getCustomerNumber().trim();
            //Check the status of the customer
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                //Log the error
                genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Decline", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }


            //validate IMEI FOR MOBILE CHANNEL
            if ("MOBILE".equalsIgnoreCase(channel)) {
                boolean imeiMatch = bCryptEncoder.matches(requestPayload.getImei(), customer.getImei());
                if (!imeiMatch) {
                    genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    genericService.createUserActivity("", "Validate IMEI", "appMessages.customer.imei.invalid", channel, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[0], Locale.ENGLISH), requestBy, 'F');
                    errorResponse.setResponseCode(ResponseCodes.IMEI_MISMATCH.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Decline", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.loan.record.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the status of the loan request
            if (!"PENDING".equalsIgnoreCase(loanRecord.getStatus())) {
                //Log the error
                genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Decline", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the loan is a digital loan
            if (!LoanCategory.BRIGHTA_LOAN.getCategoryCode().equalsIgnoreCase(loanRecord.getLoanSetup().getLoanCategory())) {
                //Log the error
                genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Decline", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Digital Loan Decline", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Digital Loan Decline", loanOptions.getApprovedAmount(), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Start a thread to send the loan email notification
            NotificationPayload emailPayload = new NotificationPayload();
            emailPayload.setMobileNumber(requestPayload.getMobileNumber());
            emailPayload.setRecipientName(customer.getLastName() + ", " + customer.getOtherName());
            emailPayload.setRecipientEmail(customer.getEmail());
            emailPayload.setRequestId(requestPayload.getRequestId());
            emailPayload.setDisbursementAccount(loanRecord.getDisbursementAccount());
            emailPayload.setLoanId(loanRecord.getLoanId());
            emailPayload.setLoanAmount(String.valueOf(loanRecord.getLoanAmountRequested()));
            emailPayload.setToken(token);
            // genericService.sendDigitalLoanAcceptanceEmail(emailPayload);

            loanRecord.setStatus("DECLINED");
//            loanRecord.setLoanAmountApproved(new BigDecimal(loanOptions.getApprovedAmount()));
//            loanRecord.setLoanTenor(loanOptions.getTenor());
//            loanRecord.setMonthlyRepayment(new BigDecimal(loanOptions.getMonthlyRepayment()));
//            loanRecord.setFirstRepaymentDate(LocalDate.now().plusDays(30));
//            loanRecord.setTotalRepayment(new BigDecimal(loanOptions.getTotalRepayment()));
//            loanRecord.setInterestRate(new BigDecimal(loanOptions.getInterestRate()));
            loanRepository.updateLoan(loanRecord);

            DigitalLoanDeclineRespondPayload loanResponse = new DigitalLoanDeclineRespondPayload();
            loanResponse.setResponseMessage("Loan Declined Successfully");

            loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            return gson.toJson(loanResponse);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Digital Loan Decline", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @SuppressWarnings("unused")
    public String digitalLoanDeclineFallback(String token, DigitalLoanDeclineRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateDigitalLoanDisbursementPayload(String token, LoanIdRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "digitalLoanDisbursementFallback")
    public String processDigitalLoanDisbursement(String token, LoanIdRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String response = "";
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Digital Loan Disbursement", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Digital Loan Disbursement", "", channel, messageSource.getMessage("appMessages.loan.record.noexist", new Object[0], Locale.ENGLISH), "", 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Customer customer = loanRepository.getCustomerUsingMobileNumber(loanRecord.getCustomer().getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the customer number
            String customerNumber = customer.getCustomerNumber().trim();
            //Check the status of the customer
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                //Log the error
                genericService.generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the status of the loan request
            if (!"ACCEPTED".equalsIgnoreCase(loanRecord.getStatus())) {
                //Log the error
                genericService.generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Digital Loan Disbursement", "", channel, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "", 'F');

                errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the loan is a digital loan
            if (!LoanCategory.BRIGHTA_LOAN.getCategoryCode().equalsIgnoreCase(loanRecord.getLoanSetup().getLoanCategory())) {
                //Log the error
                genericService.generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getCustomer().getCustomerNumber(), "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Call the disbursement API
            DisburseLoanRequestPayload disburse = new DisburseLoanRequestPayload();
            disburse.setAmount(loanRecord.getLoanAmountApproved().toString());
            disburse.setBranchCode(digitalBranchCode);
            disburse.setCategory("21068");
            disburse.setCurrency("NGN");
            disburse.setCustomerId(loanRecord.getCustomer().getCustomerNumber());
            disburse.setDrawDownAccount(loanRecord.getDisbursementAccount());
            disburse.setFrequency(String.valueOf(loanRecord.getLoanTenor()));

            //CREATE METHOD FOR INTEREST RATE
            String rate;
            if (loanRecord.getLoanTenor().equalsIgnoreCase("4")) {
                rate = "14";
            } else if (loanRecord.getLoanTenor().equalsIgnoreCase("3")) {
                rate = "12";
            } else if (loanRecord.getLoanTenor().equalsIgnoreCase("2")) {
                rate = "10";
            } else {
                rate = "8";
            }
            disburse.setInterestRate(rate);
            LocalDate valueDate = LocalDate.now();
            LocalDate maturityDate = valueDate.plusMonths(Long.parseLong(loanRecord.getLoanTenor()));
            disburse.setMaturityDate(maturityDate.toString().replace("-", ""));
            disburse.setValueDate(valueDate.toString().replace("-", ""));

            String ofsRequest = gson.toJson(disburse);

            //Generate the OFS Response log
            genericService.generateLog("Digital Loan Disbursement", token, ofsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToMiddleware("/loan/disburseDigitalLoan", ofsRequest);

            //Generate the OFS Response log
            genericService.generateLog("Digital Loan Disbursement", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            String validationResponse = genericService.validateT24Response(middlewareResponse);
            if (validationResponse != null) {
                //Log the response
                genericService.generateLog("Digital Loan Disbursement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                return gson.toJson(errorResponse);
            }

            DisburseLoanResponsePayload disbursePayload = gson.fromJson(middlewareResponse, DisburseLoanResponsePayload.class);
            if (!"00".equalsIgnoreCase(disbursePayload.getResponseCode())) {
                //Log the response
                genericService.generateLog("Digital Loan Disbursement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                loanRecord.setStatus("FAILED");
                loanRepository.updateLoan(loanRecord);

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(disbursePayload.getResponseMessage());
                return gson.toJson(errorResponse);
            }

            //Disbursement was successful
            loanRecord.setDisbursedAt(LocalDate.now());
            loanRecord.setStatus("DISBURSED");
            loanRecord.setLoanDisbursementId(disbursePayload.getContractNumber());
            loanRepository.updateLoan(loanRecord);

            LoanResponsePayload loanResponse = new LoanResponsePayload();
            loanResponse.setCustomerName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
            loanResponse.setFirstRepaymentDate(LocalDate.now().toString());
            loanResponse.setInterestRate(loanRecord.getLoanSetup().getInterestRate() + "%");
            loanResponse.setLoanAmountRequested(loanRecord.getLoanAmountRequested().toString());
            loanResponse.setLoanAmountApproved(loanRecord.getLoanAmountApproved().toString());
            loanResponse.setLoanId(loanRecord.getLoanId());
            loanResponse.setLoanDisbursementId(loanRecord.getLoanDisbursementId());
            loanResponse.setLoanTenor(loanRecord.getLoanTenor());
            loanResponse.setLoanType(loanRecord.getLoanSetup().getLoanName());
            loanResponse.setMonthlyRepayment(loanRecord.getMonthlyRepayment().toString());
            loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response = gson.toJson(loanResponse);

            //Start a thread to send the loan email notification
            NotificationPayload emailPayload = new NotificationPayload();
            emailPayload.setMobileNumber(loanRecord.getCustomer().getMobileNumber());
            emailPayload.setRecipientName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
            emailPayload.setRecipientEmail(loanRecord.getCustomer().getEmail());
            emailPayload.setRequestId(requestPayload.getRequestId());
            emailPayload.setDisbursementAccount(loanRecord.getDisbursementAccount());
            emailPayload.setLoanId(loanRecord.getLoanId());
            emailPayload.setLoanAmount(String.valueOf(loanRecord.getLoanAmountRequested()));
            emailPayload.setLoanType(loanRecord.getLoanSetup().getLoanName());
            emailPayload.setToken(token);
            genericService.sendDigitalLoanOfferEmail(emailPayload);

            //Start a thread to send SMS
            NotificationPayload smsRequest = new NotificationPayload();
            smsRequest.setInterestRate(loanRecord.getLoanSetup().getInterestRate());
            smsRequest.setTenor(Integer.valueOf(loanRecord.getLoanTenor()));
            smsRequest.setAmountApproved(new BigDecimal(loanRecord.getLoanAmountApproved().toString()));
            smsRequest.setMaturedAt(loanRecord.getMaturedAt());
            smsRequest.setMobileNumber(loanRecord.getCustomer().getMobileNumber());
            smsRequest.setDisbursementAccount(loanRecord.getDisbursementAccount());
            smsRequest.setRequestId(requestPayload.getRequestId());
            smsRequest.setToken(token);
            genericService.sendLoanSMS(smsRequest);

            //Call the thread to upload loan data to CRC Credit Bureau
            genericService.uploadToCreditBureau(token, loanRecord);
            return response;
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Digital Loan Disbursement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @SuppressWarnings("unused")
    public String digitalLoanDisbursementFallback(String token, LoanIdRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateDigitalLoanRenewalPayload(String token, LoanRenewalRequestPayload requestPayload) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    @HystrixCommand(fallbackMethod = "digital_loan_renewal_fallback")
    public String processDigitalLoanRenewal(String token, LoanRenewalRequestPayload requestPayload) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @SuppressWarnings("unused")
    public String digital_loan_renewal_fallback(String token, LoanRenewalRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateGuarantorUpdatePayload(String token, GuarantorUpdatePayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getGuarantormobile1());
        rawString.add(requestPayload.getGuarantorname1().trim());
        rawString.add(requestPayload.getGuarantormobile2());
        rawString.add(requestPayload.getGuarantorname2().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processGuarantorUpdate(String token, GuarantorUpdatePayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String response = "";
        GuarantorUpdateResponsePayload responsePayload = new GuarantorUpdateResponsePayload();
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        //Check the channel information
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("Digital Loan Guarantor", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Digital Loan Guarantor", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        if (customer == null) {
            //Log the error
            genericService.generateLog("Digital Loan Guarantor", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Loan Guarantor", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        customer.setGuarantor_1_mobile(requestPayload.getGuarantormobile1());
        customer.setGuarantor_1_name(requestPayload.getGuarantorname1());
        customer.setGuarantor_2_mobile(requestPayload.getGuarantormobile2());
        customer.setGuarantor_2_name(requestPayload.getGuarantorname2());

        Customer updateCustomer = loanRepository.updateCustomer(customer);

        responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        responsePayload.setResponseDescription("Guarantor Profile Update Successful");
        response = gson.toJson(responsePayload);
        //Log the response
        genericService.generateLog("Rubyx Loan Renewal", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.upload.success", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
        return response;

    }

    @SuppressWarnings("unused")
    public String digital_loan_Guarantor_fallback(String token, GuarantorUpdatePayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateDigitalLoanHistoryPayload(String token, DigitalLoanHistoryRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
//        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        String decryptedString = genericService.decryptString("", encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);

    }

    @Override
    public String processDigitalActiveLoan(String token, DigitalActiveLoanRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String response = "";

        //Log the request
        String requestJson = gson.toJson(requestPayload);
        //Check the channel information
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Digital Loan History", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        //Check if customer exist
        if (customer == null) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Loan History", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
        String customerNumber = customer.getMobileNumber();
        boolean loanExist = false;

        List<Account> accountRecord = loanRepository.getCustomerAccounts(customer);
        String t24Account = accountRecord.get(0).getOldAccountNumber();
        log.info("T24 Account {}", t24Account);
//        if (loanRecord != null) {
//            Schedule scheduleUpdat = loanRepository.getScheduleByMonthAndId("1", loanRecord.getLoanId());
//
//            scheduleUpdat.setPastDueAmount("200");
//
//            Schedule schedule2 = loanRepository.updateSchedule(scheduleUpdat);
//
//            Schedule scheduleUpdates = loanRepository.getScheduleByMonthAndId("1", loanRecord.getLoanId());

        //Active Loan Omnix
//            if (loanRecord.getStatus().equalsIgnoreCase("DISBURSED")) {
//                List<ScheduleResponsePayloadList> scheduleList = new ArrayList<>();
//                ScheduleResponsePayload process = new ScheduleResponsePayload();
//
//                DigitalLoanHistoryResponseList loanHistory = new DigitalLoanHistoryResponseList();
//
//                loanHistory.setAmountApproved(String.valueOf(loanRecord.getLoanAmountApproved()));
//
//                loanHistory.setResponseMessage("This is Omnix Active Loan");
//                loanHistory.setDisbursedAt(String.valueOf(loanRecord.getDisbursedAt()));
//                loanHistory.setAccountNumber(loanRecord.getDisbursementAccount());
//                loanHistory.setStatus(loanRecord.getStatus());
//                loanHistory.setLoanId(loanRecord.getLoanId());
//                loanHistory.setBookingDate(String.valueOf(loanRecord.getDisbursedAt()));
//                loanHistory.setMaturityDate(String.valueOf(loanRecord.getMaturedAt()));
//                loanHistory.setNarration("This will come from T24");
//                loanHistory.setLoanAmount(String.valueOf(loanRecord.getLoanAmountApproved()));
//                loanHistory.setPastDueAmount("T24");
//
//                List<Schedule> schedule = loanRepository.getSchedule(loanRecord.getLoanId());
//                log.info("LOAN SCHEDULE LIST {}", gson.toJson(schedule));
//                if (schedule != null) {
//                    for (Schedule schedule1 : schedule) {
//                        ScheduleResponsePayloadList schedulePayload = new ScheduleResponsePayloadList();
//                        schedulePayload.setLoanBalance(schedule1.getLoanBalance());
//                        schedulePayload.setMonth(schedule1.getMonths());
//                        schedulePayload.setEarlyRepaymentAmount(schedule1.getEarlyRepaymentAmount());
//                        schedulePayload.setPastDueAmount(schedule1.getPastDueAmount());
//                        schedulePayload.setDueDate(String.valueOf(schedule1.getRepaymentDate()));
//                        scheduleList.add(schedulePayload);
//                    }
//                }
//
//                loanHistory.setScheduleList(scheduleList);
//                loanHistoryList.add(loanHistory);
//            }


        //Active Loan T24

        //Get Loan Record with Mobile Number
        Loan loanRecord = loanRepository.getLoanUsingMobileNumber(requestPayload.getMobileNumber());
        DigitalActiveLoanResponsePayload loanHistoryResponsePayload = new DigitalActiveLoanResponsePayload();
        List<DigitalLoanHistoryResponseList> loanHistoryList = new ArrayList<>();
        if (loanRecord != null) {
            log.info("CHECK FIRST THAT LOAN IS LIQUIDATED {}", loanRecord.getStatus());
            if (loanRecord.getStatus().equalsIgnoreCase("LIQUIDATED")) {
                loanHistoryResponsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                loanHistoryResponsePayload.setResponseMessage("Loan Fully Liquidated");
                loanHistoryResponsePayload.setExistingLoan(false);
                loanHistoryResponsePayload.setLoanDetails(loanHistoryList);
            }
            return gson.toJson(loanHistoryResponsePayload);
        }


        //call loan history from T24
        LoanBalanceResponsePayload ores = getLoanBalance(t24Account, token);

        // Check Active Loan in T24, Return Loan Does not exist otherwise as all Disbursed Loans are in the CBA
        if (ores != null) {

            PaystackCardDetails paystackCardDetails = new PaystackCardDetails();
            List<ScheduleResponsePayloadList> scheduleList = new ArrayList<>();
            List<Schedule> schedule = new ArrayList<>();
            PaystackDetails paystackDetails = new PaystackDetails();

            //Call Schedule Record and Paystack Details Record if LoanRecord is not null
            if (loanRecord != null) {
                schedule = loanRepository.getSchedule(loanRecord.getLoanId());
                paystackDetails = loanRepository.getPaystackDetailsWithLoanId(loanRecord.getLoanId());
            }

            //Check if paystack details available an add to active loan object
            if (paystackDetails != null) {
                paystackCardDetails.setCardType(paystackDetails.getCardType());
                paystackCardDetails.setLast4Digit(paystackDetails.getCardLast4Digit());
                paystackCardDetails.setCardReusable(paystackDetails.isCardResuable());
                paystackCardDetails.setPaystackCardAvailable(true);
                paystackDetails.setCardType(paystackDetails.getCardType());
            }

            //Add Schedule to Active Loan Response Object
            if (schedule != null) {
                for (Schedule scheduleObject : schedule) {
                    ScheduleResponsePayloadList schedulePayload = new ScheduleResponsePayloadList();
                    schedulePayload.setLoanBalance(scheduleObject.getLoanBalance());
                    schedulePayload.setMonth(scheduleObject.getMonths());
                    schedulePayload.setEarlyRepaymentAmount(scheduleObject.getEarlyRepaymentAmount());
                    schedulePayload.setPastDueAmount(scheduleObject.getPastDueAmount());
                    schedulePayload.setStatus(scheduleObject.getStatus());
                    schedulePayload.setRepaymentAmount(scheduleObject.getRepaymentAmount());
                    schedulePayload.setRepaymentDate(String.valueOf(scheduleObject.getRepaymentDate()));
                    schedulePayload.setOutstandingBalance(scheduleObject.getOutstandingBalance());
                    scheduleList.add(schedulePayload);
                }
            }

            //Add T24 Loan History Object to the Active Loan Response
            if (ores.getLoanItemPayload() != null) {
                for (LoanItemPayload oLoanItemPayload : ores.getLoanItemPayload()) {
                    DigitalLoanHistoryResponseList loanHistory = new DigitalLoanHistoryResponseList();

                    loanHistory.setAmountApproved(oLoanItemPayload.getLoanAmount());
                    loanHistory.setDisbursedAt(oLoanItemPayload.getBookingDate());
                    loanHistory.setAccountNumber(oLoanItemPayload.getAccountNumber());
                    loanHistory.setStatus(oLoanItemPayload.getStatus());
                    loanHistory.setLoanDisbursementId(oLoanItemPayload.getLoanId());
                    loanHistory.setBookingDate(oLoanItemPayload.getBookingDate());
                    loanHistory.setMaturityDate(oLoanItemPayload.getMaturityDate());
                    loanHistory.setNarration(oLoanItemPayload.getNarration());
                    loanHistory.setLoanAmount(oLoanItemPayload.getLoanAmount());
                    loanHistory.setPastDueAmount(oLoanItemPayload.getPastDueAmount());
                    loanExist = true;
                    LocalDate currentDate = LocalDate.now();
                    if (schedule != null) {
                        List<Schedule> scheduleUpdate = new ArrayList<>();
                        Schedule schedule1 = new Schedule();
                        if (loanRecord != null) {
                            log.info("LOAN STATUS HERE {}", loanRecord.getStatus());

                            //Get Schedule By repayment Date in order to get current schedule
                            scheduleUpdate = loanRepository.getScheduleByRepaymentDateAndLoanId(loanRecord.getLoanId(), currentDate);
                            //Get current schedule by month object
                            schedule1 = scheduleUpdate.get(0);
                        }

                        if (schedule1 != null) {
                            //Check that the current schedule gotten by repayment Date is paid
                            boolean paidSchedule = schedule1.getStatus().equalsIgnoreCase("PAID");

                            //Skip the first item in the list if the status is paid and return the next one
                            Schedule selectPaidSchedule = scheduleUpdate.stream()
                                    .filter(Schedule -> paidSchedule)
                                    .skip(1)
                                    .findFirst()
                                    .orElse(null); // set to null if condition false

                            if (selectPaidSchedule != null) {
                                schedule1 = selectPaidSchedule;
                            }
                            //Get Repayment Amount, Loan Id (Not Disbursement Id) and Repayment date from the Schedule by repayment date object
                            loanHistory.setRepaymentAmount(schedule1.getRepaymentAmount());
                            loanHistory.setLoanId(schedule1.getLoanId());
                            loanHistory.setScheduleStatus(schedule1.getStatus());
                            loanHistory.setCurrentMonth(schedule1.getMonths());
                            loanHistory.setRepaymentDate(String.valueOf(schedule1.getRepaymentDate()));
                            loanHistory.setOutstandingBalance(schedule1.getOutstandingBalance());
                        }
                    }


                    loanHistory.setScheduleList(scheduleList);
                    loanHistory.setCardDetails(paystackCardDetails);
                    loanHistoryList.add(loanHistory);
                }
            }

            loanHistoryResponsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            loanHistoryResponsePayload.setResponseMessage("Loan History");
            loanHistoryResponsePayload.setExistingLoan(loanExist);
            loanHistoryResponsePayload.setLoanDetails(loanHistoryList);
            return gson.toJson(loanHistoryResponsePayload);

        } else {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.record.digital.loan.empty", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customerNumber, "Digital Loan History", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.digital.loan.empty", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.record.digital.loan.empty", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public String processPendingLoanWithMobileNumber(String token, DigitalActiveLoanRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String response = "";

        //Log the request
        String requestJson = gson.toJson(requestPayload);
        //Check the channel information
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Digital Loan History", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        if (customer == null) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Loan History", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        boolean loanExist = false;
        Loan pendingLoan = loanRepository.getPendingLoanUsingMobileNumber(requestPayload.getMobileNumber());

        LoanResponsePayload loanResponse;

        if (pendingLoan != null) {
            List<LoanOptions> loanOptions = loanRepository.getLoanOffersUsingLoanId(pendingLoan.getLoanId());
            loanResponse = new LoanResponsePayload();
            List<LoanOfferResponsePayload> loanOffers = new ArrayList<>();

            if (loanOptions != null) {
                for (LoanOptions options : loanOptions) {
                    LoanOfferResponsePayload monthlyOffer = new LoanOfferResponsePayload();
                    monthlyOffer.setApprovedAmount(options.getApprovedAmount());
                    monthlyOffer.setMonthlyRepayment(options.getMonthlyRepayment());
                    monthlyOffer.setRate(options.getInterestRate());
                    monthlyOffer.setTenor(options.getTenor() + "Month(s)");
                    monthlyOffer.setTotalRepayment(options.getTotalRepayment());
                    monthlyOffer.setOptionId(options.getLoanOptionId());
//                TODO : CHECK LIST OF OFFERS
                    String loanRepayments = options.getRepayment().replace("[", "").replace("]", "");
                    String[] loanRepaymentAmount = loanRepayments.split(",");
                    ArrayList<String> loanRepaymentList = new ArrayList<>(Arrays.asList(loanRepaymentAmount));
                    monthlyOffer.setRepayments(loanRepaymentList);

                    loanOffers.add(monthlyOffer);

                }
            }


            loanResponse.setCustomerName(customer.getLastName() + " " + customer.getOtherName());
            loanResponse.setFirstRepaymentDate(pendingLoan.getFirstRepaymentDate().toString());
            loanResponse.setLoanAmountRequested(pendingLoan.getLoanAmountRequested().toString());
            loanResponse.setLoanId(pendingLoan.getLoanId());
            loanResponse.setLoanType("Brighta Loan");
            loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            loanResponse.setLoanOffers(loanOffers);
            loanResponse.setLoanId(pendingLoan.getLoanId());

        } else {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.record.digital.loan.empty", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Digital Loan History", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.digital.loan.empty", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.record.digital.loan.empty", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
        return gson.toJson(loanResponse);
    }

    @SuppressWarnings("unused")
    public String digital_loan_history_fallback(String token, DigitalLoanHistoryRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateDigitalActiveLoanPayload(String token, DigitalActiveLoanRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
//        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        String decryptedString = genericService.decryptString("requestPayload", encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);

    }

    @Override
    public String processDigitalLoanHistory(String token, DigitalLoanHistoryRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String response = "";

        //Log the request
        String requestJson = gson.toJson(requestPayload);
        //Check the channel information
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("Digital Active Loan ", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Digital Active Loan", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        if (customer == null) {
            //Log the error
            genericService.generateLog("Digital Active Loan", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Active Loan", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
        String customerNumber = customer.getMobileNumber();
        boolean loanExist = false;
        List<DigitalLoanHistoryResponsePayload> loanHistoryList = new ArrayList<>();
        // DigitalActiveLoanResponsePayload loanHistoryList = new DigitalActiveLoanResponsePayload();
        List<Loan> loanRecord = loanRepository.getLoanUsingCustomer(customer);
        // List<Loan> loanRecord = loanRepository.getLoanUsingMobileNumber(customerNumber.toString());
        if (loanRecord != null && !loanRecord.isEmpty()) {
            loanExist = true;


            for (Loan l : loanRecord) {
                DigitalLoanHistoryResponsePayload loanHistory = new DigitalLoanHistoryResponsePayload();
                loanHistory.setAmountApproved(l.getLoanAmountApproved().toString());
                loanHistory.setDisbursedAt(l.getDisbursedAt().toString());
                loanHistory.setStatus(l.getStatus());
                loanHistory.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                loanHistory.setResponseMessage("Loan History");
                loanHistory.setCreatedAt(String.valueOf(l.getCreatedAt()));
                // Add the loan history to the loan history list
                loanHistoryList.add(loanHistory);

            }

        }

        if (!loanExist) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.record.digital.loan.empty", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customerNumber, "Digital Loan History", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.digital.loan.empty", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.record.digital.loan.empty", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        return gson.toJson(loanHistoryList);

    }

    @SuppressWarnings("unused")
    public String digital_active_loan_fallback(String token, DigitalActiveLoanRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public LoanBalanceResponsePayload getLoanBalance(String t24Account, String token) {
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        LoanBalanceResponsePayload oResPayload = new LoanBalanceResponsePayload();
        String ofsMessage;
        ofsMessage = "ENQUIRY.SELECT,,$ACCESSID/$ACCESSPASS/,E.AMFB.LOAN.BAL2,ACCOUNT.NUMBER:EQ=$ACCOUNTNO";
        ofsMessage = ofsMessage.replace("$ACCESSID", userCredentials.split("/")[0]);
        ofsMessage = ofsMessage.replace("$ACCESSPASS", userCredentials.split("/")[1]);
        ofsMessage = ofsMessage.replace("$ACCOUNTNO", t24Account);

        String sResponse = genericService.postToT24(ofsMessage);
        if (sResponse.contains("No matching record")
                || sResponse.contains("No records were found")
                || sResponse.contains("APPLICATION MISSING")
                || sResponse.contains("NO SIGN ON NAME SUPPLIED DURING SIGN ON PROCESS")
                || sResponse.contains("SECURITY VIOLATION")
                || !sResponse.contains("RESULT::RESULT")
                || sResponse.contains("No records")) {
            return oResPayload;
        }
        LoanItemPayload[] oLoanItemDto;
        try {
            if (sResponse.contains("RESULT::RESULT")) {
                sResponse = sResponse.split("RESULT::RESULT,")[1];
                List<String> rowList = Arrays.asList(sResponse.split(","));
                oLoanItemDto = new LoanItemPayload[rowList.size()];
                int k = 0;
                for (String s : rowList) {
                    s = s.split("\\t")[0];
                    s = s.replaceAll("\"", "");
                    LoanItemPayload loanItemDto = new LoanItemPayload();
                    List<String> dataList = Arrays.asList(s.split("\\*"));
                    loanItemDto.setLoanId(dataList.get(1));
                    loanItemDto.setBookingDate(dataList.get(2));
                    loanItemDto.setMaturityDate(dataList.get(3));
                    loanItemDto.setNarration(dataList.get(4));
                    loanItemDto.setAccountNumber(dataList.get(6));
                    loanItemDto.setLoanAmount(dataList.get(7));
                    loanItemDto.setBalance(dataList.get(10));
                    loanItemDto.setStatus(dataList.get(11));
                    // get past due amount
                    ofsMessage = "PD,/S/PROCESS," + userCredentials.split("\\/")[0] + "/" + userCredentials.split("\\/")[1] + "/,PD" + dataList.get(1);
                    String pdAmount = "0.00";
                    sResponse = genericService.postToT24(ofsMessage);

                    try {
                        if (sResponse.contains("/1")) {
                            pdAmount = genericService.getOfsValue(sResponse, "TOTAL.AMT.TO.REPAY");
                        }

                    } catch (Exception e) {
                    }
                    loanItemDto.setPastDueAmount(pdAmount);
                    oLoanItemDto[k] = loanItemDto;
                    k++;
                }

                oResPayload.setLoanItemPayload(oLoanItemDto);

            }

        } catch (Exception e) {

        }

        return oResPayload;

    }


    /**
     * @param paystackRef
     * @return
     * @throws UnirestException Call Paystack with the reference to get any Transaction Details including the authorization code to charge
     */
    @Override
    public PaystackTransactionDetailsResponsePayload processPaystackCardTransactionDetails(String paystackRef, String loanId) throws UnirestException {

        PaystackDetails paystackDetails = loanRepository.getPaystackDetailsWithLoanId(loanId);
        PaystackTransactionDetailsResponsePayload paystackResponse = new PaystackTransactionDetailsResponsePayload();

        if (paystackDetails != null) {
            paystackResponse.setResponseCode("03");
            paystackResponse.setResponseMessage("Paystack details already exist");
            return paystackResponse;
        }

        HttpResponse<String> apiResponse = Unirest.get(PAYSTACK_URL + paystackRef)
                .header("Authorization", "Bearer " + PAYSTACK_KEY)
                .asString();

        paystackResponse = gson.fromJson(apiResponse.getBody(), PaystackTransactionDetailsResponsePayload.class);

        //Persist Paystack Details to be returned in Active Loan
        paystackDetails = new PaystackDetails();
        paystackDetails.setLoanId(loanId);
        paystackDetails.setEmail(paystackResponse.getData().getCustomer().getEmail());
        paystackDetails.setAuthorizationCode(paystackResponse.getData().getAuthorization().getAuthorization_code());
        paystackDetails.setCardLast4Digit(paystackResponse.getData().getAuthorization().getLast4());
        paystackDetails.setCardResuable(Boolean.parseBoolean(paystackResponse.getData().getAuthorization().getReusable()));
        paystackDetails.setCardType(paystackResponse.getData().getAuthorization().getCard_type());

        loanRepository.createPaystack(paystackDetails);

        return paystackResponse;
    }

    //Call Paystack to charge Customer Card for Repayment
    public PaystackChargeCardResponsePayload processPaystackCardForLoanRepayment(String token, PaystackChargeCardRequestPayload paystackChargeCardRequestPayload) throws UnirestException {

        PaystackChargeCardResponsePayload chargeCardResponsePayload;
        String payloadJson = gson.toJson(paystackChargeCardRequestPayload, PaystackChargeCardRequestPayload.class);

        HttpResponse<String> responseString = Unirest.post(PAYSTACK_CHARGE_CARD_URL)
                .header("Authorization", "Bearer " + PAYSTACK_CHARGE_CARD_KEY)
                .header("content-type", "application/json")
                .body(payloadJson)
                .asString();

        String responseBody = responseString.getBody();

        chargeCardResponsePayload = gson.fromJson(responseBody, PaystackChargeCardResponsePayload.class);

        if (!chargeCardResponsePayload.getData().getStatus().equalsIgnoreCase("success")) {
            chargeCardResponsePayload.setResponseCode("06");
            chargeCardResponsePayload.setResponseMessage(chargeCardResponsePayload.getData().getStatus());
        }

        //Call Fund Transfer to debit pool account and credit Customer Save Brighta

        if (chargeCardResponsePayload.getStatus().equalsIgnoreCase("true") && chargeCardResponsePayload.getData().getStatus().equalsIgnoreCase("success")) {
            int amount = chargeCardResponsePayload.getData().getAmount() / 100;
//            int amount = chargeCardResponsePayload.getData().getAmount() - chargeCardResponsePayload.getData().getFees();
            String manualToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJJQiIsInJvbGVzIjoiW0FDQ09VTlRfQkFMQU5DRVMsIEFDQ09VTlRfU1RBVEVNRU5ULCBBQ0NPVU5UX0RFVEFJTFMsIEFDQ09VTlRfT1BFTklORywgQUNDT1VOVF9HTE9CQUwsIFdBTExFVF9BQ0NPVU5UX09QRU5JTkcsIEFJUlRJTUVfQ0FMTEJBQ0ssIEFJUlRJTUVfU0VMRiwgQUlSVElNRV9PVEhFUlMsIERBVEFfU0VMRiwgREFUQV9PVEhFUlMsIEFJUlRJTUVfREFUQV9ERVRBSUxTLCBCVk5fVkFMSURBVElPTiwgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBDQUJMRV9UVl9ERVRBSUxTLCBDQUJMRV9UVl9CSUxMRVIsIENSRURJVF9CVVJFQVVfVkFMSURBVElPTiwgSU5ESVZJRFVBTF9XSVRIX0JWTiwgSU5ESVZJRFVBTF9XSVRIT1VUX0JWTiwgQ09SUE9SQVRFX0NVU1RPTUVSLCBDVVNUT01FUl9CT0FSRElORywgQ1VTVE9NRVJfREVUQUlMUywgVVBEQVRFX0NVU1RPTUVSX1RJRVIsIFVQREFURV9DVVNUT01FUl9TVEFUVVMsIFVQREFURV9DVVNUT01FUl9TVEFURV9SRVNJREVOQ0UsIFVQREFURV9DVVNUT01FUl9DSVRZX1JFU0lERU5DRSwgVVBEQVRFX0NVU1RPTUVSX1JFU0lERU5USUFMX0FERFJFU1MsIFVQREFURV9DVVNUT01FUl9QQVNTV09SRCwgVVBEQVRFX0NVU1RPTUVSX1NFQ1VSSVRZX1FVRVNUSU9OLCBVUERBVEVfQ1VTVE9NRVJfUElOLCBVUERBVEVfQ1VTVE9NRVJfRU1BSUwsIFVQREFURV9DVVNUT01FUl9NQVJJVEFMX1NUQVRVUywgVVBEQVRFX0NVU1RPTUVSX01PQklMRV9OVU1CRVIsIEFVVEhfQ1VTVE9NRVJfVVNJTkdfUElOLCBBVVRIX0NVU1RPTUVSX1VTSU5HX1BBU1NXT1JELCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIEVMRUNUUklDSVRZX0JJTExfREVUQUlMUywgRUxFQ1RSSUNJVFlfQklMTEVSUywgTE9DQUxfRlVORFNfVFJBTlNGRVIsIExPQ0FMX0ZVTkRTX1RSQU5TRkVSX1dJVEhfQ0hBUkdFLCBSRVZFUlNFX0xPQ0FMX0ZVTkRTX1RSQU5TRkVSLCBPUEFZLCBQQVlfQVRUSVRVREUsIE5JUF9OQU1FX0VOUVVJUlksIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfVE9fV0FMTEVUX0ZVTkRTX1RSQU5TRkVSLCBJREVOVElUWV9WQUxJREFUSU9OLCBCT09LX0FSVElTQU5fTE9BTiwgUEVORElOR19BUlRJU0FOX0xPQU4sIERJU0JVUlNFX0FSVElTQU5fTE9BTiwgQVVUSF9BUlRJU0FOX0xPQU4sIFJFTkVXX0FSVElTQU5fTE9BTiwgQk9PS19ESUdJVEFMX0xPQU4sIERJU0JVUlNFX0RJR0lUQUxfTE9BTiwgQUNDRVBUX0RJR0lUQUxfTE9BTiwgUkVORVdfRElHSVRBTF9MT0FOLCBMT0FOX1NFVFVQLCBMT0FOX1RZUEVfTElTVElORywgU01TX05PVElGSUNBVElPTiwgVFJBTlNBQ1RJT05fRU1BSUxfQUxFUlQsIExPQU5fT0ZGRVJfRU1BSUxfQUxFUlQsIExPQU5fR1VBUkFOVE9SX0VNQUlMX0FMRVJULCBXQUxMRVRfQkFMQU5DRSwgV0FMTEVUX0RFVEFJTFMsIFdBTExFVF9DVVNUT01FUiwgQ1JFQVRFX1dBTExFVCwgQ0xPU0VfV0FMTEVULCBXQUxMRVRfQUlSVElNRV9TRUxGLCBXQUxMRVRfQUlSVElNRV9PVEhFUlMsIFdBTExFVF9EQVRBX1NFTEYsIFdBTExFVF9EQVRBX09USEVSUywgV0FMTEVUX0NBQkxFX1RWX1NVQlNDUklQVElPTiwgV0FMTEVUX0VMRUNUUklDSVRZX0JJTEwsIFdBTExFVF9UT19XQUxMRVRfRlVORFNfVFJBTlNGRVIsIFdBTExFVF9UT19BQ0NPVU5UX0ZVTkRTX1RSQU5TRkVSLCBXQUxMRVRfVE9fSU5URVJfQkFOS19UUkFOU0ZFUiwgSU5URVJfQkFOS19UT19XQUxMRVRfRlVORFNfVFJBTlNGRVIsIFdBTExFVF9JTlRFUl9CQU5LX05BTUVfRU5RVUlSWSwgQ09OVkVSVF9XQUxMRVRfVE9fQUNDT1VOVCwgREFUQV9QTEFOLCBQT1NUSU5HX1JFU1RSSUNUSU9OLCBDQVJEX1JFUVVFU1QsIEZVTkRTX1RSQU5TRkVSX1NUQVRVUywgTklCU1NfUVJfUEFZTUVOVCwgQVVUSF9TRUNVUklUWV9RVUVTVElPTiwgVEVMTEVSLCBVUERBVEVfQ1VTVE9NRVJfQlZOLCBGVU5EU19UUkFOU0ZFUl9ERUxFVEUsIEFVVEhfQ1VTVE9NRVJfVVNJTkdfRklOR0VSUFJJTlQsIFVQREFURV9DVVNUT01FUl9GSU5HRVJfUFJJTlQsIEFDQ09VTlRfQkFMQU5DRSwgR1JVUFAsIEdPQUxTX0FORF9JTlZFU1RNRU5ULCBBR0VOQ1lfQkFOS0lORywgUlVCWVhdIiwiYXV0aCI6Imt4dmQzRFNUeWV2U1I1N3M1SXM0T1E9PSIsIkNoYW5uZWwiOiJJQkFOS0lORyIsIklQIjoiMTAuMTAuMC41MiIsImlzcyI6IkFjY2lvbiBNaWNyb2ZpbmFuY2UgQmFuayIsImlhdCI6MTY4MzcyMDQxMSwiZXhwIjo2MjUxNDI4NDQwMH0.cwV77UOfXE0D0HNyQp0xAiHhY1j6aeCFGhVAn0hP6p0";
            PaystackLoanRepaymentResponse creditCustomerSavebrighta = digitalService.processPaystackCollectionFromPoolAccountToCustomerAccount(token, paystackChargeCardRequestPayload.getLoanId(), String.valueOf(amount));
            log.info("Credit Save Brighta {}", creditCustomerSavebrighta);
            if (creditCustomerSavebrighta != null) {
                if (creditCustomerSavebrighta.getResponseCode().equals("00")) {
                    chargeCardResponsePayload.setResponseCode("00");
                    chargeCardResponsePayload.setResponseMessage("Customer Savebrighta Credited successfully");
                } else {
                    chargeCardResponsePayload.setResponseCode("06");
                    chargeCardResponsePayload.setResponseMessage("Credit from Pool Account Customer Savebrighta Failed " + creditCustomerSavebrighta.getResponseMessage());

                }
            }

        }
        log.info("Charging Card in the Service Impl {}", chargeCardResponsePayload);
        return chargeCardResponsePayload;

    }


    //Endpoint that Mobile App calls for Early Repayment
    @Override
    public String processEarlyRepayment(String token, EarlyRepaymentRequestPayload requestPayload) throws UnirestException, ParseException {
        EarlyRepaymentResponsePayload earlyRepaymentResponsePayload = new EarlyRepaymentResponsePayload();
        log.info("Early Repayment Request Payload {}", requestPayload);


        //Check if Loan is fully repaid
        Loan loanRecord= loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
        if(loanRecord == null){
            earlyRepaymentResponsePayload.setResponseCode("03");
            earlyRepaymentResponsePayload.setResponseMessage("No Loan Record found for " + requestPayload.getLoanId());
            log.info("Early Repayment Response Payload {}", gson.toJson(earlyRepaymentResponsePayload));
            return gson.toJson(earlyRepaymentResponsePayload);
        }
        if(loanRecord.getStatus().equalsIgnoreCase("LIQUIDATED")){
            earlyRepaymentResponsePayload.setResponseCode("00");
            earlyRepaymentResponsePayload.setResponseMessage("The loan " + loanRecord.getLoanDisbursementId() +" has been LIQUIDATED. Kindly apply for a new loan");
            log.info("Early Repayment Response Payload {}", gson.toJson(earlyRepaymentResponsePayload));
            return gson.toJson(earlyRepaymentResponsePayload);
        }

        LocalDate currentDate = LocalDate.now();
        List<Schedule> schedule = loanRepository.getScheduleByRepaymentDateAndLoanId(requestPayload.getLoanId(), currentDate);
        //Check if schedule by current and status is not null
        if (schedule == null) {
            earlyRepaymentResponsePayload.setResponseCode("03");
            earlyRepaymentResponsePayload.setResponseMessage("No Schedule Record found for " + requestPayload.getLoanId());
            log.info("Early Repayment Response Payload {}", gson.toJson(earlyRepaymentResponsePayload));
            return gson.toJson(earlyRepaymentResponsePayload);
        }

        double balance = 0;
        String dueAmount = schedule.get(0).getPastDueAmount();
        balance = (Double.parseDouble(schedule.get(0).getRepaymentAmount()) + Double.parseDouble(dueAmount)) - Double.parseDouble(requestPayload.getAmount());
        log.info("Balance {}", balance);
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String balanceFormat = decimalFormat.format(balance);

        // If repayment method is card, call charge paystack via card
        PaystackChargeCardResponsePayload cardResponsePayload = new PaystackChargeCardResponsePayload();
        if (requestPayload.getPaymentMethod().trim().equalsIgnoreCase("CARD")) {
            PaystackDetails paystackDetailsWithLoanId = loanRepository.getPaystackDetailsWithLoanId(requestPayload.getLoanId());
            log.info("Paystack  Details with Loan ID {}", paystackDetailsWithLoanId);
            PaystackChargeCardRequestPayload paystackChargeCardRequestPayload = new PaystackChargeCardRequestPayload();
            if (paystackDetailsWithLoanId == null) {
                earlyRepaymentResponsePayload.setResponseCode("06");
                earlyRepaymentResponsePayload.setResponseMessage("Error While charging Paystack for Loan Repayments");
                return gson.toJson(earlyRepaymentResponsePayload);
            }
            paystackChargeCardRequestPayload.setEmail(paystackDetailsWithLoanId.getEmail());
            paystackChargeCardRequestPayload.setAuthorization_code(paystackDetailsWithLoanId.getAuthorizationCode());
            double amountPaystack = Double.parseDouble(requestPayload.getAmount()) * 100;
            paystackChargeCardRequestPayload.setAmount(String.valueOf(amountPaystack));
            paystackChargeCardRequestPayload.setLoanId(requestPayload.getLoanId());

            // Call paystack and charge card
            cardResponsePayload = digitalService.processPaystackCardForLoanRepayment(token, paystackChargeCardRequestPayload);
            log.info("Charge Paystack Response {}", cardResponsePayload);
            if (!cardResponsePayload.getResponseCode().equals("00")) {
                earlyRepaymentResponsePayload.setResponseMessage(cardResponsePayload.getResponseMessage());
                earlyRepaymentResponsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                return gson.toJson(earlyRepaymentResponsePayload);
            }
        }

        /* Call lien endpoint and pass amount to it if fund transfer from pool account was successful */
        PlaceLienResponsePayload placeLien = new PlaceLienResponsePayload();
        boolean callLien = false;
        if (requestPayload.getPaymentMethod().equals("CARD")) {
            if (cardResponsePayload.getResponseCode().equals("00")) {
                callLien = true;
            }
        }
        if (requestPayload.getPaymentMethod().equalsIgnoreCase("ACCOUNT")) {
            callLien = true;
        }

        if (callLien) {
            LockAmountDto requestLockAmount = new LockAmountDto();
            requestLockAmount.setAmount(requestPayload.getAmount());
            requestLockAmount.setAccountNumber(schedule.get(0).getDisbursementAccount());
            requestLockAmount.setLoanId(requestPayload.getLoanId());
            requestLockAmount.setStartDate(LocalDate.now());
            requestLockAmount.setEndDate(LocalDate.now().plusYears(10));
            placeLien = digitalService.processPlaceLien(token, requestLockAmount);
        }

        if (placeLien.getResponseCode().equals("00")) {
            // Update outstanding Balance in the schedule
            List<Schedule> totalSchedule = loanRepository.getSchedule(requestPayload.getLoanId());
            double outsBalDouble = 0;
            for (Schedule updateOutstandingBalance : totalSchedule) {
                String outsBal = updateOutstandingBalance.getRepaymentAmount();
                outsBalDouble = outsBalDouble + Double.parseDouble(outsBal);
                updateOutstandingBalance.setOutstandingBalance(outsBal);
            }
            for (Schedule updateOutstandingBalance : totalSchedule) {
                updateOutstandingBalance.setOutstandingBalance(String.valueOf(outsBalDouble));
                loanRepository.updateSchedule(updateOutstandingBalance);

            }

            //Update early repayment amount and repayment amount in schedule table after successfully placing lien
            schedule.get(0).setEarlyRepaymentAmount(requestPayload.getAmount());
            schedule.get(0).setRepaymentAmount(String.valueOf(balanceFormat));
            if (Double.parseDouble(schedule.get(0).getRepaymentAmount()) <= 0.00) {
                schedule.get(0).setStatus("Paid");
            }
            loanRepository.updateSchedule(schedule.get(0));
            //call early liquidation and liquidate if loan fully paid
            String liquidateLoanTemp = digitalService.processLiquidateLoanTemp(token, schedule.get(0).getLoanId());
            LiquidateLoanResponsePayload liquidate = gson.fromJson(liquidateLoanTemp, LiquidateLoanResponsePayload.class);
            earlyRepaymentResponsePayload.setResponseMessage(requestPayload.getAmount() + " has Successfully been Placed on Lien for Customer with Account " + schedule.get(0).getDisbursementAccount() + " for early repayment "+ liquidate.getResponseMessage());
            earlyRepaymentResponsePayload.setResponseCode("00");

        } else {
            earlyRepaymentResponsePayload.setResponseMessage(placeLien.getResponseMessage());
            earlyRepaymentResponsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
        }
        log.info("Early Repayment Response Payload {}", gson.toJson(earlyRepaymentResponsePayload));
        return gson.toJson(earlyRepaymentResponsePayload);
    }

    //Method that sends SMS
    @Override
    public SMSResponsePayload sendSms(SMS newSMS) {

        SMSPayload oSMSPayload = new SMSPayload();
        oSMSPayload.setMessage(newSMS.getMessage());
        oSMSPayload.setMobileNumber(newSMS.getMobileNumber());
        String ofsRequest = gson.toJson(oSMSPayload);

        String middlewareResponse = genericService.postToSMSService("/sms/send", ofsRequest);

        SMS createSMS = smsRepository.createSMS(newSMS);


        System.out.println("Middleware response: " + middlewareResponse);

        SMSResponsePayload smsmResponsePayload = gson.fromJson(middlewareResponse, SMSResponsePayload.class);
        if ("00".equals(smsmResponsePayload.getResponseCode())) {
            createSMS.setStatus("SUCCESS");
            smsRepository.updateSMS(createSMS);

            log.info("Middleware response shows that SMS was sent here: {}", middlewareResponse);

            SMSResponsePayload smsResponse = new SMSResponsePayload();
            smsResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            smsResponse.setMobileNumber(newSMS.getMobileNumber());
            smsResponse.setMessage(newSMS.getMessage());
            smsResponse.setSmsFor(newSMS.getSmsFor());
            gson.toJson(smsResponse);

        } else {
            createSMS.setStatus("FAILED");
            createSMS.setFailureReason(smsmResponsePayload.getResponseDescription());
            smsRepository.updateSMS(createSMS);
            //Create activity log
            genericService.generateLog("SMS", "", "Success", "API Response", "INFO", newSMS.getMobileNumber());
        }
        return smsmResponsePayload;
    }

    @Override
    public Double flatRateLoanApplication(String approvedAmount, double interestRate, int tenor) {

        double monthlyInterestRate = (interestRate / 100) * 12; // Monthly interest rate

        double monthlyRepayment = (Double.parseDouble(approvedAmount) + (Double.parseDouble(approvedAmount) * monthlyInterestRate * tenor)) / (Double.parseDouble(approvedAmount));
        log.info("Monthly Repayment in the Service {} ", monthlyRepayment);
        return monthlyRepayment;
    }


    //This method places lien on an account
    @Override
    public PlaceLienResponsePayload processPlaceLien(String token, LockAmountDto request) throws ParseException {

        PlaceLienResponsePayload lienResponsePayload = new PlaceLienResponsePayload();

        //Call the Account microservice
        String accountBalanceRequestId = genericService.generateTransRef("ACB");
        AccountNumberPayload accBalRequest = new AccountNumberPayload();
        accBalRequest.setAccountNumber(request.getAccountNumber());
        accBalRequest.setRequestId(accountBalanceRequestId);
        accBalRequest.setToken(token);
        accBalRequest.setHash(genericService.hashAccountBalanceRequest(accBalRequest));
        String accBalRequestJson = gson.toJson(accBalRequest);
        //Log the request for account balance
        genericService.generateLog("Place Lien - Account Balance", token, accBalRequestJson, "Account Balance", "INFO", request.getRequestId());
        String accBalResponseJson = accountService.accountBalance(token, accBalRequestJson);
        genericService.generateLog("Place Lien - Account Balance", token, accBalResponseJson, "Account Balance", "INFO", request.getRequestId());
        AccountBalanceResponsePayload accBalResponse = gson.fromJson(accBalResponseJson, AccountBalanceResponsePayload.class);
        log.info("Account Balance Response {}", gson.toJson(accBalResponse));
        log.info("Available Balance Response {}", accBalResponse.getAvailableBalance());
        if (accBalResponse.getAvailableBalance().contains("-")) {
            lienResponsePayload.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode());
            lienResponsePayload.setResponseMessage("Account " + request.getAccountNumber() + " does not have enough balance");
            return lienResponsePayload;
        }
        if (Double.parseDouble(accBalResponse.getAvailableBalance().replace(",", "")) < Double.parseDouble(request.getAmount())) {
            lienResponsePayload.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode());
            lienResponsePayload.setResponseMessage("Account " + request.getAccountNumber() + " does not have enough balance");
            return lienResponsePayload;
        }

        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        DecimalFormat myFormatter = new DecimalFormat("###.##");
        String sAmount1 = myFormatter.format(Double.parseDouble(request.getAmount()));

        Date fromDate = dateFormat.parse(String.valueOf(request.getStartDate()));
        Date toDate = dateFormat.parse(String.valueOf(request.getEndDate()));

        String OFS = "AC.LOCKED.EVENTS,NEW/I/PROCESS//0,";
        String t24User = userCredentials.split("/")[0];

        String t24Pass = userCredentials.split("/")[1];
        OFS = OFS + t24User + "/" + t24Pass + "/,";
        OFS = OFS + ",ACCOUNT.NUMBER::=" + request.getAccountNumber();
        OFS = OFS + ",LOCKED.AMOUNT::=" + sAmount1;
        OFS = OFS + ",FROM.DATE::=" + dateFormat.format(fromDate);//20130326
        OFS = OFS + ",TO.DATE::=" + dateFormat.format(toDate);//20130326
        String sResponse = genericService.postToT24(OFS);

        log.info("OFS RESPONSE FOR PLACING LIEN {}", sResponse);

        PlaceLien createLien = new PlaceLien();
        if (sResponse.contains("/1")) {
            String lockId = sResponse.substring(0, sResponse.indexOf("/"));
            lienResponsePayload.setAmount(request.getAmount());
            lienResponsePayload.setStatus("SUCCESS");
            lienResponsePayload.setLoanId(request.getLoanId());
            lienResponsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            lienResponsePayload.setResponseMessage("Lien Placed Successfully");


            createLien.setAmount(lienResponsePayload.getAmount());
            createLien.setStatus(lienResponsePayload.getStatus());
            createLien.setLoanId(lienResponsePayload.getLoanId());
            createLien.setLockId(lockId);

            loanRepository.createLienRecord(createLien);
        } else if (sResponse.contains("Ws Error")) {
            createLien.setStatus("FAILED");
            createLien.setFailureReason("Error Occurred while performing operation");
            loanRepository.updateLienRecord(createLien);
            lienResponsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            lienResponsePayload.setResponseMessage("Error Occurred while performing operation");
        } else {
            createLien.setStatus("FAILED");
            createLien.setFailureReason("Error Occurred while performing operation");
            loanRepository.updateLienRecord(createLien);
            lienResponsePayload.setResponseCode(ResponseCodes.CORRUPT_DATA.getResponseCode());
            lienResponsePayload.setResponseMessage("Error Occurred");
        }

        return lienResponsePayload;
    }


    //This moves money from the pool account to Customer's Savebrighta Account for Loan Repayment
    @Override
    public PaystackLoanRepaymentResponse processPaystackCollectionFromPoolAccountToCustomerAccount(String token, String loanId, String amount) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String requestId = genericService.generateTransRef("TS");
        String debitAccount = "NGN1045100010001";
        PaystackLoanRepaymentResponse paystackLoanRepaymentResponse = new PaystackLoanRepaymentResponse();

        try {
            PaystackLoanRepaymentRecord createPaystackLoanRepaymentRecord = new PaystackLoanRepaymentRecord();
            Loan loanRecord = loanRepository.getLoanUsingLoanId(loanId);

            //Generate the funds transfer request payload
            LocalTransferWithInternalPayload ftRequestPayload = new LocalTransferWithInternalPayload();

            ftRequestPayload.setMobileNumber(loanRecord.getMobileNumber());
            ftRequestPayload.setDebitAccount(debitAccount); //Update in Config File later
            ftRequestPayload.setCreditAccount(loanRecord.getDisbursementAccount());
            ftRequestPayload.setAmount(amount);
            ftRequestPayload.setNarration(loanRecord.getCustomerBusiness() + " LOAN REPAYMENT " + loanRecord.getDisbursementAccount());
            ftRequestPayload.setTransType("ACTF");
            ftRequestPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
            ftRequestPayload.setInputter(requestBy + "-" + loanRecord.getMobileNumber());
            ftRequestPayload.setAuthorizer(requestBy + "-" + loanRecord.getMobileNumber());
            ftRequestPayload.setNoOfAuthorizer("0");
            ftRequestPayload.setRequestId(requestId);
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash(genericService.hashLocalTransferValidationRequest(ftRequestPayload));

            //Create the request payload JSON
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Call the funds transfer microservices
            String ftResponseJson = ftService.localTransfer(token, ftRequestJson);
            FundsTransferResponsePayload ftResponsePayload = gson.fromJson(ftResponseJson, FundsTransferResponsePayload.class);
            log.info("Funds Transfer Request Payload from Pool Account to Savebrighta Payload {}", gson.toJson(ftRequestPayload));
            log.info("Funds Transfer Response {}", ftResponsePayload);

            //Create payment record
            createPaystackLoanRepaymentRecord.setLoanId(loanRecord.getLoanId());
            createPaystackLoanRepaymentRecord.setCreditAccount(loanRecord.getDisbursementAccount());
            createPaystackLoanRepaymentRecord.setDebitAccount(debitAccount);
            createPaystackLoanRepaymentRecord.setCreatedAt(LocalDate.now());
            createPaystackLoanRepaymentRecord.setT24TransRef(ftResponsePayload.getT24TransRef());
            createPaystackLoanRepaymentRecord.setAmount(amount);
            //Set status if transaction successful
            if (ftResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                createPaystackLoanRepaymentRecord.setStatus("SUCCESS");
                paystackLoanRepaymentResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                paystackLoanRepaymentResponse.setResponseMessage("Customer debited the sum of " + amount + " successfully");
            }
            //Set status if transaction failed
            else {
                OmniResponsePayload ftErrorResponse = gson.fromJson(ftResponseJson, OmniResponsePayload.class);
                createPaystackLoanRepaymentRecord.setStatus("FAILED");
                createPaystackLoanRepaymentRecord.setFailureReason(ftErrorResponse.getResponseMessage());
                //Log the error
                genericService.generateLog("Loan repayment with card", token, ftErrorResponse.getResponseMessage(), "API Response", "INFO", requestId);
                //Create User Activity log
                genericService.createUserActivity("", "Loan repayment with card", "", channel, ftErrorResponse.getResponseMessage(), requestBy, 'F');
                paystackLoanRepaymentResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                paystackLoanRepaymentResponse.setResponseMessage(ftErrorResponse.getResponseMessage());
            }
            loanRepository.createPaystackLoanRepaymentRecord(createPaystackLoanRepaymentRecord);
        } catch (Exception e) {
            e.getLocalizedMessage();
            e.printStackTrace();
        }
        return paystackLoanRepaymentResponse;
    }

    @Override
    public String processCustomerLoanDetails(String token, DigitalLoanHistoryRequestPayload requestPayload) {

        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String response = "";
        LoanDetailsResponsePayload loanDetailsResponsePayload = new LoanDetailsResponsePayload();
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        //Check the channel information
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Digital Loan History", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
        //Check customer
        Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        if (customer == null) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Loan History", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
        //Get Loan Details
        Loan loanDetails = loanRepository.getLoanUsingMobileNumber(requestPayload.getMobileNumber());
        //Check if loan details null
        if (loanDetails == null) {
            //Log the error
            genericService.generateLog("Digital Loan History", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Digital Loan Details", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }
        //Get Paystack Card Details
        PaystackDetails paystackDetails = loanRepository.getPaystackDetailsWithLoanId(loanDetails.getLoanId());
        //Check if Paystack Details is null
        if (paystackDetails == null) {
            //Log the error
            genericService.generateLog("Paystack Loan Details", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Paystack Loan Details", requestPayload.getMobileNumber(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        //Set Loan Details Response

        loanDetailsResponsePayload.setMobileNumber(requestPayload.getMobileNumber());
        loanDetailsResponsePayload.setGuarantorMobile1(customer.getGuarantor_1_mobile());
        loanDetailsResponsePayload.setGuarantorMobile2(customer.getGuarantor_2_mobile());
        loanDetailsResponsePayload.setGuarantorName1(customer.getGuarantor_1_name());
        loanDetailsResponsePayload.setGuarantorName2(customer.getGuarantor_2_name());
        loanDetailsResponsePayload.setNameOfSpouse(loanDetails.getNameOfSpouse());
        loanDetailsResponsePayload.setNoOfDependent(loanDetails.getNoOfDependent());
        loanDetailsResponsePayload.setMaritalStatus(loanDetails.getMaritalStatus());
        loanDetailsResponsePayload.setBusinessName(loanDetails.getBusinessName());
        loanDetailsResponsePayload.setBusinessType(loanDetails.getBusinessType());
        loanDetailsResponsePayload.setBusinessAddress(loanDetails.getBusinessAddress());
        loanDetailsResponsePayload.setTypeOfResidence(loanDetails.getTypeOfResidence());
        loanDetailsResponsePayload.setYearOfResidency(loanDetails.getYearOfResidence());


        return gson.toJson(loanDetailsResponsePayload);
    }

    @Override
    public String processLiquidateLoanTemp(String token, String loanId) throws ParseException {

        String userCredentials = jwtToken.getUserCredentialFromToken(token);

        String OFS = "LD.LOANS.AND.DEPOSITS,EARLY.MAT/I/PROCESS//1,";
        String liquidateVersion = "LD.LOANS.AND.DEPOSITS,EARLY.MAT/I/PROCESS//1";


        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        DecimalFormat myFormatter = new DecimalFormat("###.##");

        LocalDate currentDate = LocalDate.now();
        Date matDate = dateFormat.parse(String.valueOf("20230606"));

        String t24User = userCredentials.split("/")[0];
        String t24Pass = userCredentials.split("/")[1];

//        t24User = "IIDIKA";
//        t24Pass = "Password123";

        String ofsRequest = liquidateVersion.trim() + "," + userCredentials + "/,," + "LD2315708630";
        OFS = OFS + t24User + "/" + t24Pass + "/," + ",LD2315708630";
//        OFS = OFS + ",@ID::=" + "LD2315708630";
//        OFS = OFS + ",CUSTOMER.ID::=" + "516555";
//        OFS = OFS + ",CATEGORY::=" + "21057";
//        OFS = OFS + ",FIN.MAT.DATE::=" + dateFormat.format(matDate);//20130326
//        OFS = OFS + ",DRAWDOWN.ACCOUNT::=" + "1999200429";
//        OFS = OFS + ",MODIFY.TRM::=" + "Got";
//        OFS = OFS + ",EARLY.MAT.REASN::=" + "Got enough Money";
//
//        String sResponse = genericService.postToT24(OFS);
//        log.info("OFS REQUEST {}", OFS);
//        log.info("OFS RESPONSE FOR EARLY MATURITY {}", sResponse);

        //Check Schedule status
        List<Schedule> schedule = loanRepository.getSchedule(loanId);
        Loan loanRecord = loanRepository.getLoanUsingLoanId(loanId);

        LiquidateLoanResponsePayload liquidateLoan = new LiquidateLoanResponsePayload();
        int lastSchedule = schedule.size() - 1;
        if (schedule.get(lastSchedule).getStatus().equalsIgnoreCase("Paid") && schedule.get(lastSchedule).getPastDueAmount().startsWith("0")) {
            loanRecord.setStatus("LIQUIDATED");
            loanRepository.updateLoan(loanRecord);
            liquidateLoan.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            liquidateLoan.setResponseMessage("Loan Liquidated Successfully");
        } else {
            liquidateLoan.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode());
            liquidateLoan.setResponseMessage("Loan yet to be liquidated");
        }
        log.info("RESPONSE FOR EARLY LIQUIDATION {}", gson.toJson(liquidateLoan));
        return gson.toJson(liquidateLoan);
    }
}
