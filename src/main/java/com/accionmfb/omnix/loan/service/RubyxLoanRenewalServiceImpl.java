/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.model.Account;
import com.accionmfb.omnix.loan.model.AppUser;
import com.accionmfb.omnix.loan.model.Branch;
import com.accionmfb.omnix.loan.model.Customer;
import com.accionmfb.omnix.loan.model.Loan;
import com.accionmfb.omnix.loan.model.RubyxLoanRenewal;
import com.accionmfb.omnix.loan.payload.AccountBalanceResponsePayload;
import com.accionmfb.omnix.loan.payload.AccountNumberPayload;
import com.accionmfb.omnix.loan.payload.CustomerNumberRequestPayload;
import com.accionmfb.omnix.loan.payload.DisburseLoanRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanResponsePayload;
import com.accionmfb.omnix.loan.payload.OmniResponsePayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanDisbursementPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalApplyPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalListResponsePayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalQueryPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalUpdatePayload;
import com.accionmfb.omnix.loan.repository.LoanRepository;
import com.google.gson.Gson;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class RubyxLoanRenewalServiceImpl implements RubyxLoanRenewalService {

    @Autowired
    GenericService genericService;
    @Autowired
    AccountService accountService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    LoanRepository loanRepository;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    ServletContext servletContext;
    @Autowired
    CustomerService customerService;
    @Value("${omnix.version.scorecard.view}")
    private String scorecardViewVersion;
    @Value("${omnix.version.scorecard.update}")
    private String scorecardUpdateVersion;
    @Value("${omnix.version.customer}")
    private String customerVersion;

    @Override
    public Object rubyxLoanRenewalCheckIfSameRequestId(String requestId) {
        try {
            RubyxLoanRenewal loanRecord = loanRepository.getRubyxRenewalUsingRequestId(requestId);
            return loanRecord == null;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public boolean validateRubyxLoanRenewalPayload(String token, RubyxLoanRenewalPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getCustomerNumber().trim());
        rawString.add(requestPayload.getAccountOfficer().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getProductCode().trim());
        rawString.add(requestPayload.getRenewalScore().trim());
        rawString.add(requestPayload.getRenewalRating().trim());
        rawString.add(requestPayload.getRenewalAmount().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processRubyxLoanRenewal(String token, RubyxLoanRenewalPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response;
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Rubyx Loan Renewal", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check if the request comes with same details
            RubyxLoanRenewal loanRecord = loanRepository.getRubyxRenewalUsingDetails(requestPayload.getCustomerNumber(), requestPayload.getRenewalScore(), requestPayload.getRenewalRating());
            if (loanRecord != null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.exist", new Object[]{requestPayload.getCustomerNumber(), requestPayload.getRenewalRating(), requestPayload.getRenewalScore()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getCustomerNumber(), "Rubyx Loan Renewal", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestPayload.getCustomerNumber(), requestPayload.getRenewalRating(), requestPayload.getRenewalScore()}, Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.exist", new Object[]{requestPayload.getCustomerNumber(), requestPayload.getRenewalRating(), requestPayload.getRenewalScore()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getCustomerNumber(), "Rubyx Loan Renewal", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Fetch the Customer BVN and Mobile Number details and update the record          
            Customer customer = loanRepository.getCustomerUsingCustomerNumber(requestPayload.getCustomerNumber());
            if (customer == null) {
                //Log the response
                genericService.generateLog("Rubyx Loan Renewal", token, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                responsePayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH));
                response = gson.toJson(responsePayload);
                return response;
            }

            //Call the profile thread
            profileRubyxLoanRenewal(requestPayload, customer, userCredentials, token, requestPayload.getRequestId());

            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.upload.success", new Object[0], Locale.ENGLISH));
            response = gson.toJson(responsePayload);
            //Log the response
            genericService.generateLog("Rubyx Loan Renewal", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.upload.success", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Rubyx Loan Renewal", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

    @Async
    private CompletableFuture<String> profileRubyxLoanRenewal(RubyxLoanRenewalPayload requestPayload, Customer customer, String userCredentials, String token, String requestId) {
        //Persist record to DB
        RubyxLoanRenewal newLoanRenewal = new RubyxLoanRenewal();
        newLoanRenewal.setAccountOfficerCode(requestPayload.getAccountOfficer());
        newLoanRenewal.setBranch(requestPayload.getBranchCode());
        newLoanRenewal.setCreatedAt(LocalDateTime.now());
        newLoanRenewal.setCustomerNumber(requestPayload.getCustomerNumber());
        newLoanRenewal.setInterestRate(""); //Defaulted
        newLoanRenewal.setRenewalAmount(requestPayload.getRenewalAmount());
        newLoanRenewal.setRenewalRating(requestPayload.getRenewalRating());
        newLoanRenewal.setRequestId(requestPayload.getRequestId());
        newLoanRenewal.setRenewalScore(requestPayload.getRenewalScore());
        newLoanRenewal.setProductCode(requestPayload.getProductCode());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate date = LocalDate.parse(requestPayload.getEligibilityEndDate(), formatter);
        newLoanRenewal.setTenor("");  //To be supplied by the customer during the application 
        LocalDate currentDate = LocalDate.now();
        if (currentDate.isAfter(date)) {
            newLoanRenewal.setStatus("EXPIRED");
            newLoanRenewal.setFailureReason("Loan renewal offer has expired");
            genericService.generateLog("Rubyx Loan Renewal", token, "Loan renewal offer has expired.", "API Response", "INFO", requestId);
            loanRepository.createRubyxLoanRenewal(newLoanRenewal);
            return CompletableFuture.completedFuture("EXPIRED");
        }
        newLoanRenewal.setStatus("PENDING");
        RubyxLoanRenewal createLoanRenewal = loanRepository.createRubyxLoanRenewal(newLoanRenewal);

        //Fetch the customer score card
        String ofsRequest = scorecardViewVersion.trim() + "," + userCredentials
                + "/NG0010001," + customer.getCustomerNumber();

        String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
        //Generate the OFS Response log
        genericService.generateLog("Rubyx Loan Renewal", token, newOfsRequest, "OFS Request", "INFO", requestId);
        String scorecardResponse = genericService.postToT24(ofsRequest);
        //Generate the OFS Response log
        genericService.generateLog("Rubyx Loan Renewal", token, scorecardResponse, "OFS Response", "INFO", requestId);
        String validationResponse = genericService.validateT24Response(scorecardResponse);
        if (validationResponse != null) {
            //Log the error
            genericService.generateLog("Rubyx Loan Renewal", token, scorecardResponse, "API Response", "INFO", requestId);
            createLoanRenewal.setStatus("FAILED");
            createLoanRenewal.setFailureReason("Unable to retrieve score card");
            loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
        } else {
            ofsRequest = customerVersion.trim().replace("/I/", "/S/") + "," + userCredentials
                    + "/NG0010001," + customer.getCustomerNumber();
            newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Rubyx Loan Renewal", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            //Post to T24
            String loanCycleResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Rubyx Loan Renewal", token, loanCycleResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            validationResponse = genericService.validateT24Response(loanCycleResponse);
            if (validationResponse != null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal", token, loanCycleResponse, "API Response", "INFO", requestId);
                createLoanRenewal.setStatus("FAILED");
                createLoanRenewal.setFailureReason("Unable to retrieve customer details");
                loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
            } else {
                String scoreCard = genericService.getTextFromOFSResponse(scorecardResponse, "CRD.SCORE:1:1");
                int loanCycle = Integer.valueOf(genericService.getTextFromOFSResponse(loanCycleResponse, "CYCLE:1:1"));
                double interestRate = 0;
                if ((scoreCard.equalsIgnoreCase("A") || scoreCard.equalsIgnoreCase("AA"))) {
                    BigDecimal amountGreaterThan1m = new BigDecimal("1000000");
                    BigDecimal amountTorenew = new BigDecimal(newLoanRenewal.getRenewalAmount());
                    if (loanCycle < 3) {
                        interestRate = 6;
                    } else if (loanCycle >= 3 && loanCycle <= 4) {
                        interestRate = 5.5;
                    } else if (loanCycle >= 5 && loanCycle <= 7) {
                        interestRate = 5.5;
                    } else if (loanCycle >= 8 && amountTorenew.compareTo(amountGreaterThan1m) > 0) {
                        interestRate = 5;
                    } else if (loanCycle >= 8 && amountTorenew.compareTo(amountGreaterThan1m) < 0) {
                        interestRate = 5.5;
                    }
                } else {
                    interestRate = 6;
                }

                //Determine the interest rate
                createLoanRenewal.setCustomer(customer);
                createLoanRenewal.setStatus("SUCCESS");
                createLoanRenewal.setCurrentLoanCycle(loanCycle);
                createLoanRenewal.setInterestRate(String.valueOf(interestRate));
                createLoanRenewal.setMobileNumber(customer.getMobileNumber());
                loanRepository.updateRubyxLoanRenewal(createLoanRenewal);

                //get customer bvn
                ofsRequest = customerVersion.trim().replace("/I/", "/S/") + "," + userCredentials
                        + "/" + "/NG0010001," + customer.getCustomerNumber();
                newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                //Generate the OFS Response log
                genericService.generateLog("Customer Upload", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                //Post to T24
                String bvnResponse = genericService.postToT24(ofsRequest);
                validationResponse = genericService.validateT24Response(bvnResponse);
                if (validationResponse != null) {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal", token, loanCycleResponse, "API Response", "INFO", requestId);
                    createLoanRenewal.setStatus("FAILED");
                    createLoanRenewal.setFailureReason("Unable to retrieve customer details");
                    loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                } else {
                    String bvn = genericService.getTextFromOFSResponse(bvnResponse, "BVN:1:1");

                    //call bvn service to validate bvn
                    if (bvn == null || bvn.equalsIgnoreCase("")) {
                        createLoanRenewal.setStatus("FAILED");
                        createLoanRenewal.setFailureReason(messageSource.getMessage("appMessages.bvn.notexist", new Object[0], Locale.ENGLISH));
                        loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                    } else {
                        createLoanRenewal.setBvn(bvn);
                        loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                    }
                }

                //Check if customer number is set
                if (!customer.getCustomerNumber().matches("[0-9]{1,}")) {

                    //check t24 for customer number
                    createLoanRenewal.setStatus("FAILED");
                    createLoanRenewal.setFailureReason(messageSource.getMessage("appMessages.customer.number.notexist", new Object[0], Locale.ENGLISH));
                    loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                }

                //Check if customer mobile number is set
                if (!customer.getMobileNumber().matches("[0-9]{11}")) {
                    createLoanRenewal.setStatus("FAILED");
                    createLoanRenewal.setFailureReason(messageSource.getMessage("appMessages.customer.mobile.notexist", new Object[0], Locale.ENGLISH));
                    loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                }

                //Get all the accounts of the customer
                List<Account> accountList = loanRepository.getCustomerAccounts(customer);
                String brightCommitmentAccount = "";
                if (accountList == null) {
                    //No account, hence no Brighta Commitment account available
                    createLoanRenewal.setStatus("FAILED");
                    createLoanRenewal.setFailureReason(messageSource.getMessage("appMessages.account.brighta.commitment.notexist", new Object[0], Locale.ENGLISH));
                    loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                } else {
                    for (Account acc : accountList) {
                        //Check if any of the account is Brighta Commitment with category 6005
                        if (acc.getCategory().equalsIgnoreCase("6005")) {
                            brightCommitmentAccount = acc.getAccountNumber();
                        }
                    }

                    if (brightCommitmentAccount.equalsIgnoreCase("")) {
                        createLoanRenewal.setStatus("FAILED");
                        createLoanRenewal.setFailureReason(messageSource.getMessage("appMessages.account.brighta.commitment.notexist", new Object[0], Locale.ENGLISH));
                        loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                    } else {
                        createLoanRenewal.setBrightaCommitmentAccount(brightCommitmentAccount);
                        loanRepository.updateRubyxLoanRenewal(createLoanRenewal);
                    }
                }
                return CompletableFuture.completedFuture("Success");
            }
        }
        return CompletableFuture.completedFuture("SUCCESS");
    }

    @Override
    public boolean validateRubyxLoanRenewalUpdatePayload(String token, RubyxLoanRenewalUpdatePayload requestPayload
    ) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getUpdateType().trim());
        rawString.add(requestPayload.getUpdateValue().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processRubyxLoanRenewalUpdate(String token, RubyxLoanRenewalUpdatePayload requestPayload
    ) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        String requestBy = jwtToken.getUsernameFromToken(token);
        LoanResponsePayload responsePayload = new LoanResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Rubyx Loan Renewal Update", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the record exist
            RubyxLoanRenewal loanRecord = loanRepository.getRubyxRenewalUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.notexist", new Object[]{"Mobile Number ", requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.notexist", new Object[]{"Mobile Number ", requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the customer profile is completed
            if (!loanRecord.getStatus().equalsIgnoreCase("SUCCESS")) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.incomplete", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.incomplete", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            if (requestPayload.getUpdateType().equalsIgnoreCase("Customer")) {
                if (requestPayload.getUpdateValue().equalsIgnoreCase("True") || requestPayload.getUpdateValue().equalsIgnoreCase("False")) {
                    loanRecord.setCustomerSignOfferLetter(Boolean.valueOf(requestPayload.getUpdateValue()));
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            if (requestPayload.getUpdateType().equalsIgnoreCase("Guarantor")) {
                if (requestPayload.getUpdateValue().equalsIgnoreCase("True") || requestPayload.getUpdateValue().equalsIgnoreCase("False")) {
                    loanRecord.setGuarantorSignOfferLetter(Boolean.valueOf(requestPayload.getUpdateValue()));
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            if (requestPayload.getUpdateType().equalsIgnoreCase("Id Validation")) {
                if (requestPayload.getUpdateValue().equalsIgnoreCase("True") || requestPayload.getUpdateValue().equalsIgnoreCase("False")) {
                    loanRecord.setGuarantorIdVerified(Boolean.valueOf(requestPayload.getUpdateValue()));
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check if disbursement account is set
            if (requestPayload.getUpdateType().equalsIgnoreCase("Disbursement Account")) {
                if (requestPayload.getUpdateValue().matches("[0-9]{10}")) {
                    loanRecord.setDisbursementAccount(requestPayload.getUpdateValue());
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"10 digit NUBAN"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"10 digit NUBAN"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check if Brighta commitment account is set
            if (requestPayload.getUpdateType().equalsIgnoreCase("Brighta Commitment")) {
                if (requestPayload.getUpdateValue().matches("[0-9]{10}")) {
                    loanRecord.setBrightaCommitmentAccount(requestPayload.getUpdateValue());
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"10 digit NUBAN"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"10 digit NUBAN"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check if loan cycle is set
            if (requestPayload.getUpdateType().equalsIgnoreCase("Loan Cycle")) {
                if (requestPayload.getUpdateValue().matches("[0-9]{1,2}")) {
                    loanRecord.setCurrentLoanCycle(Integer.valueOf(requestPayload.getUpdateValue()));
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"1 or 2 digit loan cycle"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"1 or 2 digit loan cycle"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check if interest rate is set
            if (requestPayload.getUpdateType().equalsIgnoreCase("Interest Rate")) {
                if (requestPayload.getUpdateValue().matches("[0-9.]{1,}")) {
                    loanRecord.setInterestRate(requestPayload.getUpdateValue());
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"1 digit with fraction interest rate"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"1 digit with fraction interest rate"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check if credit bureau is set
            if (requestPayload.getUpdateType().equalsIgnoreCase("Credit Bureau")) {
                if (requestPayload.getUpdateValue().equalsIgnoreCase("True") || requestPayload.getUpdateValue().equalsIgnoreCase("False")) {
                    loanRecord.setCreditBureauSearchDone(Boolean.valueOf(requestPayload.getUpdateValue()));
                    loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
                } else {
                    //Log the error
                    genericService.generateLog("Rubyx Loan Renewal Update", token, messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.value.invalid", new Object[]{"True or False"}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }

            //Check if the Customer and Guarantor have signed
            if (loanRecord.isCustomerSignOfferLetter() && loanRecord.isGuarantorSignOfferLetter() && loanRecord.isGuarantorIdVerified()) {
                loanRecord.setStatus("ACCEPTED");
                loanRecord = loanRepository.updateRubyxLoanRenewal(loanRecord);
            }

            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setCustomerName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
            responsePayload.setInterestRate(loanRecord.getInterestRate());
            responsePayload.setLoanAmountApproved(loanRecord.getRenewalAmount());
            responsePayload.setMobileNumber(loanRecord.getMobileNumber());
            responsePayload.setLoanTenor(loanRecord.getTenor());
            responsePayload.setTotalLoanBalance(String.valueOf(loanRecord.getTotalLoanBalance()));
            responsePayload.setTotalLoans(String.valueOf(loanRecord.getNoOfTotalLoans()));
            responsePayload.setTotalPerformingLoanBalance(String.valueOf(loanRecord.getTotalPerformingBalance()));
            responsePayload.setTotalPerformingLoans(String.valueOf(loanRecord.getNoOfPerformingLoans()));
            responsePayload.setStatus(loanRecord.getStatus());
            responsePayload.setLoanScore(loanRecord.getRenewalScore());
            responsePayload.setLoanRating(loanRecord.getRenewalRating());
            responsePayload.setCustomerSigned(String.valueOf(loanRecord.isCustomerSignOfferLetter()));
            responsePayload.setGuarantorSigned(String.valueOf(loanRecord.isGuarantorSignOfferLetter()));
            responsePayload.setCustomerApplied(String.valueOf(loanRecord.isCustomerApply()));
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Rubyx Loan Renewal Update", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

    @Override
    public boolean validateCustomerNumberPayload(String token, CustomerNumberRequestPayload requestPayload
    ) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getCustomerNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processRubyxLoanRenewalFetch(String token, CustomerNumberRequestPayload requestPayload
    ) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        String requestBy = jwtToken.getUsernameFromToken(token);
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Rubyx Loan Renewal Fetch", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Fetch", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the record exist
            RubyxLoanRenewal loanRecord = loanRepository.getRubyxRenewalUsingCustomerNumber(requestPayload.getCustomerNumber());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Fetch", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.notexist", new Object[]{"Customer Number ", requestPayload.getCustomerNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.notexist", new Object[]{"Customer Number ", requestPayload.getCustomerNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            LoanResponsePayload responsePayload = new LoanResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setCustomerName(loanRecord.getCustomer().getLastName() + " " + loanRecord.getCustomer().getOtherName());
            responsePayload.setInterestRate(loanRecord.getInterestRate());
            responsePayload.setLoanAmountApproved(loanRecord.getRenewalAmount());
            responsePayload.setMobileNumber(loanRecord.getCustomer().getMobileNumber());
            responsePayload.setLoanTenor(loanRecord.getTenor());
            responsePayload.setTotalLoanBalance(String.valueOf(loanRecord.getTotalLoanBalance()));
            responsePayload.setTotalLoans(String.valueOf(loanRecord.getNoOfTotalLoans()));
            responsePayload.setTotalPerformingLoanBalance(String.valueOf(loanRecord.getTotalPerformingBalance()));
            responsePayload.setTotalPerformingLoans(String.valueOf(loanRecord.getNoOfPerformingLoans()));
            responsePayload.setStatus(loanRecord.getStatus());
            responsePayload.setLoanScore(loanRecord.getRenewalScore());
            responsePayload.setLoanRating(loanRecord.getRenewalRating());
            responsePayload.setCustomerSigned(String.valueOf(loanRecord.isCustomerSignOfferLetter()));
            responsePayload.setGuarantorSigned(String.valueOf(loanRecord.isGuarantorSignOfferLetter()));
            responsePayload.setCustomerApplied(String.valueOf(loanRecord.isCustomerApply()));
            responsePayload.setLoanId(String.valueOf(loanRecord.getId()));
            responsePayload.setGuarantorIdVerified(String.valueOf(loanRecord.isGuarantorIdVerified()));
            responsePayload.setCreditBureauSearchDone(String.valueOf(loanRecord.isCreditBureauSearchDone()));
            responsePayload.setLoanCycle(String.valueOf(loanRecord.getCurrentLoanCycle()));
            responsePayload.setProductCode(loanRecord.getProductCode());
            responsePayload.setAccountOfficerCode(loanRecord.getAccountOfficerCode());
            responsePayload.setBvn(loanRecord.getBvn());
            responsePayload.setDisbursementAccount(loanRecord.getDisbursementAccount());
            responsePayload.setCustomerNumber(loanRecord.getCustomerNumber());
            responsePayload.setBrightaCommitmentAccount(loanRecord.getBrightaCommitmentAccount());
            responsePayload.setLoanAmount(loanRecord.getLoanAmount());
            responsePayload.setResidenceAddress(loanRecord.getCustomer().getResidenceAddress());
            responsePayload.setResidenceCity(loanRecord.getCustomer().getResidenceCity());
            responsePayload.setResidenceState(loanRecord.getCustomer().getResidenceState());
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Rubyx Loan Renewal Fetch", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

    @Override
    public boolean validateRubyxLoanRenewalApplyPayload(String token, RubyxLoanRenewalApplyPayload requestPayload
    ) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getLoanAmount().trim());
        rawString.add(requestPayload.getLoanTenor().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processRubyxLoanRenewalApply(String token, RubyxLoanRenewalApplyPayload requestPayload
    ) {
        String response = "";
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Rubyx Loan Renewal Application", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Rubyx Loan Renewal Application", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if the account number is valid
            Account account = loanRepository.getAccountUsingAccountNumber(requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Rubyx Loan Renewal Application", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check for customer account for the debit
            Account customerAccount = loanRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
            if (customerAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.account.mismatch", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.customer.account.mismatch", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Rubyx Loan Renewal Application", "", channel, messageSource.getMessage("appMessages.customer.account.mismatch", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if the account belongs to Brighta Plus (1003)
            if (!account.getCategory().equalsIgnoreCase("1003")) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.product.notallowed", new Object[]{account.getCategory()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.product.notallowed", new Object[]{account.getCategory()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Rubyx Loan Renewal Application", "", channel, messageSource.getMessage("appMessages.product.notallowed", new Object[]{account.getCategory()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if the record exist
            RubyxLoanRenewal loanRecord = loanRepository.getRubyxRenewalUsingCustomerNumber(customer.getCustomerNumber());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.notexist", new Object[]{"Customer Number ", customer.getCustomerNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.notexist", new Object[]{"Customer Number ", customer.getCustomerNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the customer has applied already
            if (loanRecord.isCustomerApply()) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.applied", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.applied", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the credit bureau search has been done
            if (!loanRecord.isCreditBureauSearchDone()) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.creditbureau.notdone", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.creditbureau.notdone", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the status of the loan renewal application
            if (!loanRecord.getStatus().equalsIgnoreCase("SUCCESS")) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.incomplete", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.incomplete", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the loan amount limit
            double maxLoanAmount = Double.valueOf(loanRecord.getRenewalAmount());
            double appliedLoanAmount = Double.valueOf(requestPayload.getLoanAmount());
            if (appliedLoanAmount > maxLoanAmount) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.outofrange.amount", new Object[]{loanRecord.getRenewalAmount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Rubyx Loan Renewal Application", String.valueOf(loanRecord.getRenewalAmount()), channel, messageSource.getMessage("appMessages.rubyx.loan.renewal.outofrange.amount", new Object[]{loanRecord.getRenewalAmount()}, Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.outofrange.amount", new Object[]{loanRecord.getRenewalAmount()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the tenor of the loan entered. Should be between 4 and 12 months
            int loanTenor = Integer.valueOf(requestPayload.getLoanTenor());
            if (loanTenor < 4 || loanTenor > 12) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.outofrange.tenor", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Rubyx Loan Renewal Application", String.valueOf(loanRecord.getRenewalAmount()), channel, messageSource.getMessage("appMessages.rubyx.loan.renewal.outofrange.tenor", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.outofrange.tenor", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Rubyx Loan Renewal Application", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Rubyx Loan Renewal Application", String.valueOf(loanRecord.getRenewalAmount()), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the record as customer has applied
            loanRecord.setCustomerApply(true);
            loanRecord.setDisbursementAccount(requestPayload.getAccountNumber());
            loanRecord.setLoanAmount(requestPayload.getLoanAmount());
            loanRecord.setTenor(requestPayload.getLoanTenor());
            loanRepository.updateRubyxLoanRenewal(loanRecord);

            //Return response
            LoanResponsePayload responsePayload = new LoanResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setCustomerName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
            responsePayload.setInterestRate(loanRecord.getInterestRate());
            responsePayload.setLoanAmountApproved(loanRecord.getRenewalAmount());
            responsePayload.setMobileNumber(loanRecord.getCustomer().getMobileNumber());
            responsePayload.setLoanTenor(loanRecord.getTenor());
            responsePayload.setTotalLoanBalance(String.valueOf(loanRecord.getTotalLoanBalance()));
            responsePayload.setTotalLoans(String.valueOf(loanRecord.getNoOfTotalLoans()));
            responsePayload.setTotalPerformingLoanBalance(String.valueOf(loanRecord.getTotalPerformingBalance()));
            responsePayload.setTotalPerformingLoans(String.valueOf(loanRecord.getNoOfPerformingLoans()));
            responsePayload.setStatus(loanRecord.getStatus());
            responsePayload.setLoanScore(loanRecord.getRenewalScore());
            responsePayload.setLoanRating(loanRecord.getRenewalRating());
            responsePayload.setCustomerSigned(String.valueOf(loanRecord.isCustomerSignOfferLetter()));
            responsePayload.setGuarantorSigned(String.valueOf(loanRecord.isGuarantorSignOfferLetter()));
            responsePayload.setCustomerApplied(String.valueOf(loanRecord.isCustomerApply()));
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Rubyx Loan Renewal Application", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

    @Override
    public boolean validateRubyxLoanRenewalQueryPayload(String token, RubyxLoanRenewalQueryPayload requestPayload
    ) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getStartDate().trim());
        rawString.add(requestPayload.getEndDate().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processRubyxLoanRenewalList(String token, RubyxLoanRenewalQueryPayload requestPayload
    ) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        String requestBy = jwtToken.getUsernameFromToken(token);
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Rubyx Loan Renewal Query", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(requestPayload.getStartDate());
            Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(requestPayload.getEndDate());
            List<RubyxLoanRenewal> loanList = loanRepository.getRubyxLoanRenewalForPeriod(startDate, endDate);
            if (loanList == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.rubyx.loan.renewal.norecord", new Object[]{requestPayload.getStartDate() + "-" + requestPayload.getEndDate()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);
                //Log the response
                genericService.generateLog("Rubyx Loan Renewal Query", token, messageSource.getMessage("appMessages.rubyx.loan.renewal.norecord", new Object[]{requestPayload.getStartDate() + "-" + requestPayload.getEndDate()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                return response;
            }

            //Check if product code is supplied
            if (requestPayload.getProductCode() != null && !requestPayload.getProductCode().equalsIgnoreCase("")) {
                loanList = loanList.stream().filter(t -> t.getProductCode().equalsIgnoreCase(requestPayload.getProductCode())).collect(Collectors.toList());
            }

            //Check if the customer number filter is set
            if (requestPayload.getCustomerNumber() != null && !requestPayload.getCustomerNumber().equalsIgnoreCase("")) {
                loanList = loanList.stream().filter(t -> t.getCustomerNumber().equalsIgnoreCase(requestPayload.getCustomerNumber())).collect(Collectors.toList());
            }

            //Check if the customer apply filter is set
            if (requestPayload.getCustomerApply() != null && requestPayload.getCustomerApply().equalsIgnoreCase("True")) {
                loanList = loanList.stream().filter(t -> t.isCustomerApply() == true).collect(Collectors.toList());

            }

            //Check if the customer sign offer letter filter is set
            if (requestPayload.getCustomerSignOfferLetter() != null && requestPayload.getCustomerSignOfferLetter().equalsIgnoreCase("True")) {
                loanList = loanList.stream().filter(t -> t.isCustomerSignOfferLetter() == true).collect(Collectors.toList());
            }

            //Check if the guarantor sign offer letter filter is set
            if (requestPayload.getGuarantorSignOfferLetter() != null && requestPayload.getGuarantorSignOfferLetter().equalsIgnoreCase("True")) {
                loanList = loanList.stream().filter(t -> t.isGuarantorSignOfferLetter() == true).collect(Collectors.toList());
            }

            //Check if the credit bureau search done filter is set
            if (requestPayload.getCreditBureauSearchDone() != null && requestPayload.getCreditBureauSearchDone().equalsIgnoreCase("True")) {
                loanList = loanList.stream().filter(t -> t.isCreditBureauSearchDone() == true).collect(Collectors.toList());
            }

            //Check if the guarantor ID verified filter is set
            if (requestPayload.getGuarantorIdVerified() != null && requestPayload.getGuarantorIdVerified().equalsIgnoreCase("True")) {
                loanList = loanList.stream().filter(t -> t.isGuarantorIdVerified() == true).collect(Collectors.toList());
            }

            //Check if the loan cycle filter is set
            if (requestPayload.getLoanCycle() != null && !requestPayload.getLoanCycle().equalsIgnoreCase("")) {
                loanList = loanList.stream().filter(t -> t.getCurrentLoanCycle() == Integer.valueOf(requestPayload.getLoanCycle())).collect(Collectors.toList());
            }

            List<LoanResponsePayload> responseList = new ArrayList<>();
            for (RubyxLoanRenewal l : loanList) {
                LoanResponsePayload newResponse = new LoanResponsePayload();
                BeanUtils.copyProperties(l, newResponse);
                responseList.add(newResponse);
            }
            RubyxLoanRenewalListResponsePayload responsePayload = new RubyxLoanRenewalListResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setRubyxLoanData(responseList);

            String responseJson = gson.toJson(responsePayload);
            return responseJson;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Rubyx Loan Renewal Query", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

    @Override
    public boolean validateRubyxLoanRenewalDisbursementPayload(String token, RubyxLoanDisbursementPayload requestPayload
    ) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String disburseRubyxLoanRenewal(String token, RubyxLoanDisbursementPayload requestPayload
    ) {
//        LoanSetup loanType = loanRepository.getLoanTypeUsingCategory("RUBYX");
        AppUser appUser = loanRepository.getAppUserUsingUsername(jwtToken.getUsernameFromToken(token));
        //Check if the record exist
        RubyxLoanRenewal loanRecord = loanRepository.getRubyxRenewalUsingLoanId(requestPayload.getLoanId());
        if (loanRecord != null && loanRecord.isCustomerApply() && loanRecord.isCustomerSignOfferLetter()
                && loanRecord.isGuarantorSignOfferLetter() && loanRecord.isGuarantorIdVerified()
                && loanRecord.getStatus().equalsIgnoreCase("ACCEPTED")) {

            Branch branch = loanRepository.getBranchUsingBranchCode(loanRecord.getBranch());
            boolean equityContributionOk = false;
            String accountBalanceRequestId = genericService.generateTransRef("ACB");
            //Call the Account microservices
            AccountNumberPayload accBalRequest = new AccountNumberPayload();
            accBalRequest.setAccountNumber(loanRecord.getBrightaCommitmentAccount());
            accBalRequest.setRequestId(accountBalanceRequestId);
            accBalRequest.setToken(token);
            accBalRequest.setHash(genericService.hashAccountBalanceRequest(accBalRequest));
            String accBalRequestJson = gson.toJson(accBalRequest);
            //Log the request for account balance
            genericService.generateLog("Rubyx Loan Renewal Disbursement - Account Balance", token, accBalRequestJson, "Cron Job - OFS Request", "INFO", loanRecord.getRequestId());
            String accBalResponseJson = accountService.accountBalance(token, accBalRequestJson);
            genericService.generateLog("Rubyx Loan Renewal Disbursement - Account Balance", token, accBalResponseJson, "Cron Job - OFS Response", "INFO", loanRecord.getRequestId());
            AccountBalanceResponsePayload accBalResponse = gson.fromJson(accBalResponseJson, AccountBalanceResponsePayload.class);

            if (accBalResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                //Check if the customer has the required equity contributions
                double tenPercent = 0.1 * Double.valueOf(loanRecord.getLoanAmount());
                double fivePercent = 0.05 * Double.valueOf(loanRecord.getLoanAmount());
                double accountBalance = Double.valueOf(accBalResponse.getAvailableBalance().replace(",", ""));
                if (loanRecord.getCurrentLoanCycle() >= 2 && loanRecord.getCurrentLoanCycle() <= 5) {
                    //Get the account balance. Requires 10% equity contribution
                    if (accountBalance >= tenPercent) {
                        equityContributionOk = true;
                    }
                }

                if (loanRecord.getCurrentLoanCycle() >= 6 && loanRecord.getCurrentLoanCycle() <= 12) {
                    //Get the account balance. Requires 5% equity contribution
                    if (accountBalance >= fivePercent) {
                        equityContributionOk = true;
                    }
                }

                if (loanRecord.getCurrentLoanCycle() > 12) {
                    equityContributionOk = true;
                }

                //Check if equity contribution is successful
                if (equityContributionOk) {
                    genericService.generateLog("Rubyx Loan Renewal Disbursement - Brighta Commitment Equity Contribution", token, "OK", "Cron Job", "INFO", loanRecord.getRequestId());

                    //Check if the loan has been disbursed already
                    Loan disbursedLoan = loanRepository.getLoanUsingRequestId(loanRecord.getRequestId());
                    if (disbursedLoan != null) {
                        loanRecord.setStatus("FAILED");
                        loanRecord.setFailureReason("Loan with request id " + loanRecord.getRequestId() + " disbursed already");
                        loanRepository.updateRubyxLoanRenewal(loanRecord);
                        return gson.toJson(loanRecord);
                    } else {
                        //Take the Admin and Insurance fee. 1% each of approved loan amount
                        double adminFee = 0.01D * Double.valueOf(loanRecord.getLoanAmount());
                        double insuranceFee = 0.01D * Double.valueOf(loanRecord.getLoanAmount());

                        //Call the disbursement API
                        DisburseLoanRequestPayload disburse = new DisburseLoanRequestPayload();
                        disburse.setAmount(loanRecord.getLoanAmount());
                        disburse.setAdminFee(true);
                        disburse.setBranchCode(loanRecord.getBranch());
                        disburse.setCategory(loanRecord.getProductCode());
                        disburse.setCurrency("NGN");
                        disburse.setCustomerId(loanRecord.getCustomer().getCustomerNumber());
                        disburse.setDrawDownAccount(loanRecord.getDisbursementAccount());
                        disburse.setFrequency(String.valueOf(loanRecord.getTenor()));
                        disburse.setInterestRate(String.valueOf(loanRecord.getInterestRate()));
                        disburse.setInsuranceFee(true);
                        LocalDate valueDate = LocalDate.now();
                        LocalDate maturityDate = valueDate.plusMonths(Long.valueOf(loanRecord.getTenor()));
                        disburse.setMaturityDate(maturityDate.toString().replace("-", ""));
                        disburse.setValueDate(valueDate.toString().replace("-", ""));
                        disburse.setRubyxLoan("AUTO");
                        String ofsRequest = gson.toJson(disburse);

                        //Generate the OFS Response log
                        genericService.generateLog("Rubyx Loan Renewal Disbursement", token, ofsRequest, "Cron Job - OFS Request", "INFO", loanRecord.getRequestId());

                        String middlewareResponse = genericService.postToMiddleware("/loan/disburseDigitalLoan", ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Rubyx Loan Renewal Disbursement", token, middlewareResponse, "Cron Job - OFS Response", "INFO", loanRecord.getRequestId());
                        LoanResponsePayload responsePayload = gson.fromJson(middlewareResponse, LoanResponsePayload.class);
                        if (!responsePayload.getResponseCode().equalsIgnoreCase("00")) {
                            loanRecord.setStatus("FAILED");
                            loanRecord.setFailureReason(responsePayload.getResponseDescription());
                            loanRepository.updateRubyxLoanRenewal(loanRecord);
                            return gson.toJson(loanRecord);
                        } else {
                            //Update the Rubyx loan renewal 
                            loanRecord.setStatus("DISBURSED");
                            loanRecord.setFailureReason("");
                            loanRepository.updateRubyxLoanRenewal(loanRecord);

                            //Get the loan id from the CBA
                            String[] contractId = responsePayload.getResponseDescription().split("contractNumber:");
                            String loanDisbursementId = contractId[0].replace("Successful", "")
                                    .replace("LD", "").replace("is", "").replace(",", "");
                            //Update the loan application. Disbursement was successful
                            Loan newLoan = new Loan();
                            newLoan.setAppUser(appUser);
                            newLoan.setAdminFee(String.valueOf(adminFee));
                            newLoan.setBranch(branch);
                            newLoan.setCreatedAt(LocalDateTime.now());
                            newLoan.setCustomer(loanRecord.getCustomer());
                            newLoan.setCustomerBusiness("RUBYX LOAN RENEWAL");
                            newLoan.setDisbursedAt(LocalDate.now());
                            newLoan.setDisbursementAccount(loanRecord.getDisbursementAccount());
                            newLoan.setFirstRepaymentDate(LocalDate.now());
                            newLoan.setInsuranceFee(String.valueOf(insuranceFee));
                            newLoan.setLiquidatedAt(LocalDate.parse("1900-01-01"));
                            newLoan.setLoanAmountApproved(new BigDecimal(loanRecord.getRenewalAmount()));
                            newLoan.setLoanAmountRequested(new BigDecimal(loanRecord.getLoanAmount()));
                            newLoan.setLoanDisbursementId(loanDisbursementId != null ? "LD" + loanDisbursementId.trim() : "");
                            newLoan.setLoanId(loanDisbursementId != null ? "LD" + loanDisbursementId.trim() : "");
                            newLoan.setLoanPurpose("RUBYX LOAN RENEWAL");
//                            newLoan.setLoanSetup(loanType);
                            newLoan.setLoanTenor(loanRecord.getTenor());
                            newLoan.setMaturedAt(LocalDate.parse("1900-01-01"));
                            newLoan.setMobileNumber(loanRecord.getMobileNumber());
                            newLoan.setMonthlyRepayment(new BigDecimal(0));
                            newLoan.setProductCode(loanRecord.getProductCode());
                            newLoan.setRequestId(loanRecord.getRequestId());
                            newLoan.setStatus("SUCCESS");
                            newLoan.setTotalRepayment(BigDecimal.ZERO);
                            newLoan.setInterestRate(BigDecimal.ZERO);
                            newLoan.setTimePeriod(genericService.getTimePeriod());
                            newLoan.setSelectionScore(loanRecord.getRenewalScore());
                            newLoan.setSelectionScoreRating(loanRecord.getRenewalRating());
                            newLoan.setLimitRange("0");
                            newLoan.setMsmeScore(String.valueOf("0"));
                            loanRepository.createLoan(newLoan);

                            responsePayload.setResponseCode("00");
                            responsePayload.setLoanDisbursementId("LD" + loanDisbursementId.trim());

                            return gson.toJson(responsePayload);

                        }
                    }
                } else {
                    LoanResponsePayload responsePayload = new LoanResponsePayload();
                    genericService.generateLog("Rubyx Loan Renewal Disbursement - Brighta Commitment Equity Contribution", token, "NO", "Cron Job", "INFO", loanRecord.getRequestId());
                    //Equity contribution not okay
                    loanRecord.setStatus("FAILED");
                    loanRecord.setFailureReason("Failed to Execute Brighta Commitment Deposit Contribution");
                    loanRepository.updateRubyxLoanRenewal(loanRecord);
                    responsePayload.setResponseCode("03");
                    responsePayload.setResponseDescription("Failed to Execute Brighta Commitment Deposit Contribution");
                    return gson.toJson(responsePayload);
                }
            } else {
                genericService.generateLog("Rubyx Loan Renewal Disbursement - Account Balance", token, accBalResponseJson, "Cron Job", "INFO", loanRecord.getRequestId());
                //Equity contribution not okay
                loanRecord.setStatus("FAILED");
                loanRecord.setFailureReason("Failed to Execute Brighta Commitment Deposit Contribution");
                loanRepository.updateRubyxLoanRenewal(loanRecord);
                return gson.toJson(loanRecord);
            }
        }
        return "";
    }

}
