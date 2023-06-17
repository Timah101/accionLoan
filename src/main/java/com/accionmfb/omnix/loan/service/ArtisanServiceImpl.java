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
import com.accionmfb.omnix.loan.model.BVN;
import com.accionmfb.omnix.loan.model.Customer;
import com.accionmfb.omnix.loan.model.Identification;
import com.accionmfb.omnix.loan.model.Loan;
import com.accionmfb.omnix.loan.model.LoanSetup;
import com.accionmfb.omnix.loan.payload.ArtisanLoanRequestPayload;
import com.accionmfb.omnix.loan.payload.BVNRequestPayload;
import com.accionmfb.omnix.loan.payload.BVNResponsePayload;
import com.accionmfb.omnix.loan.payload.DisburseLoanRequestPayload;
import com.accionmfb.omnix.loan.payload.DisburseLoanResponsePayload;
import com.accionmfb.omnix.loan.payload.IdentityRequestPayload;
import com.accionmfb.omnix.loan.payload.IdentityResponsePayload;
import com.accionmfb.omnix.loan.payload.LoanIdRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanRenewalRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanResponsePayload;
import com.accionmfb.omnix.loan.payload.NotificationPayload;
import com.accionmfb.omnix.loan.payload.OmniResponsePayload;
import com.accionmfb.omnix.loan.payload.PendingArtisanLoanPayload;
import com.accionmfb.omnix.loan.payload.PortfolioPayload;
import com.accionmfb.omnix.loan.repository.LoanRepository;
import com.google.gson.Gson;
import com.mashape.unirest.http.Unirest;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class ArtisanServiceImpl implements ArtisanService {

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
    @Value("${omnix.digital.branch.code}")
    private String digitalBranchCode;
    @Value("${omnix.artisan.loan.code}")
    private String artisanLoanCode;
    @Value("${omnix.version.loan.query}")
    private String loanVersion;
    @Value("${omnix.loan.artisan.rollover.100percent.threshold}")
    private String rollover100Percent;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    Gson gson;
    @Autowired
    ApplicationContext applicationContext;

    @Override
    public boolean validateArtisanLoanBookingPayload(String token, ArtisanLoanRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getBvn().trim());
        rawString.add(requestPayload.getLoanType());
        rawString.add(requestPayload.getLoanAmount().trim());
        rawString.add(requestPayload.getLoanTenor().trim());
        rawString.add(requestPayload.getLoanPurpose().trim());
        rawString.add(requestPayload.getDisbursementAccount().trim());
        rawString.add(requestPayload.getCustomerBusiness().trim());
        rawString.add(requestPayload.getIdType().trim());
        rawString.add(requestPayload.getIdNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "artisanLoanBookingFallback")
    public String processArtisanLoanBooking(String token, ArtisanLoanRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String response = "";
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Artisan Loan Booking", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the customer number
            String customerNumber = customer.getCustomerNumber().trim();
            //Check the status of the customer
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check for customer account for the disbursement
            Account disbursementAccount = loanRepository.getCustomerAccount(customer, requestPayload.getDisbursementAccount());
            if (disbursementAccount == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(disbursementAccount.getAccountNumber(), "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if there is a loan record
            List<Loan> loanRecord = loanRepository.getLoanUsingCustomer(customer);
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
                genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.loan.pending", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.pending", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.pending", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the loan type selected
            LoanSetup loanType = loanRepository.getLoanTypeUsingCategory(requestPayload.getLoanType());
            if (loanType == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.loan.noexist", new Object[]{requestPayload.getLoanType()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.noexist", new Object[]{requestPayload.getLoanType()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.noexist", new Object[]{requestPayload.getLoanType()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the customer has existing loan in the domicile branch
            Object loanBalances = genericService.getLoanBalances(customer.getBranch().getBranchCode(), customer.getCustomerNumber(), userCredentials);
            if (!(loanBalances instanceof String)) {
                List<PortfolioPayload> loanList = (List<PortfolioPayload>) loanBalances;
                if (loanList != null) {
                    //Log the error
                    genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.loan.exist", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(customerNumber, "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.exist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                    errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.exist", new Object[0], Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
            }
            //Check the applied loan amount
            BigDecimal loanAmt = new BigDecimal(requestPayload.getLoanAmount());
            if (loanAmt.compareTo(loanType.getMinAmount()) < 0 || loanAmt.compareTo(loanType.getMaxAmount()) > 0) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, messageSource.getMessage("appMessages.loan.outofrange", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, messageSource.getMessage("appMessages.loan.outofrange", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.OUT_OF_RANGE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.outofrange", new Object[]{loanType.getMinAmount(), loanType.getMaxAmount()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Call the Identity check validation API
            Unirest.setTimeouts(0, 0);
            IdentityRequestPayload identityPayload = new IdentityRequestPayload();
            identityPayload.setFirstName(customer.getOtherName());
            identityPayload.setIdNumber(requestPayload.getIdNumber());
            identityPayload.setIdType(requestPayload.getIdType());
            identityPayload.setLastName(customer.getLastName());
            identityPayload.setMobileNumber(requestPayload.getMobileNumber());
            identityPayload.setRequestId(genericService.generateRequestId());
            identityPayload.setToken(token);
            identityPayload.setHash(genericService.hashIdentityValidationRequest(identityPayload));
            String identityRequestPayload = gson.toJson(identityPayload);

            String identityResponseJson = identityService.identityValidation(token, identityRequestPayload);
            IdentityResponsePayload identityResponsePayload = gson.fromJson(identityResponseJson, IdentityResponsePayload.class);
            if (!identityResponsePayload.getResponseCode().trim().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, identityResponseJson, "API Response", "INFO", requestPayload.getRequestId());
                return identityResponseJson;
            }

            //Call the BVN validation API
            Unirest.setTimeouts(0, 0);
            BVNRequestPayload bvnPayload = new BVNRequestPayload();
            bvnPayload.setBvn(requestPayload.getBvn());
            bvnPayload.setRequestId(genericService.generateRequestId());
            bvnPayload.setToken(token);
            bvnPayload.setHash(genericService.hashBVNValidationRequest(bvnPayload));
            String bvnRequestPayload = gson.toJson(bvnPayload);

            String bvnResponseJson = bvnService.bvnValidation(token, bvnRequestPayload);
            BVNResponsePayload bvnResponsePayload = gson.fromJson(bvnResponseJson, BVNResponsePayload.class);
            if (!bvnResponsePayload.getResponseCode().trim().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                //Log the error
                genericService.generateLog("Artisan Loan Booking", token, bvnResponseJson, "API Response", "INFO", requestPayload.getRequestId());
                return bvnResponseJson;
            }

            //Update the customer information with the details
            BVN bvn = loanRepository.getBVN(requestPayload.getBvn());
            Identification identity = loanRepository.getIdentityUsingIdNumber(requestPayload.getIdNumber());
            customer.setBvn(bvn);
            if ("Passport".equalsIgnoreCase(requestPayload.getIdType())) {
                customer.setPassport(identity);
            }
            if ("PVC".equalsIgnoreCase(requestPayload.getIdType())) {
                customer.setPvc(identity);
            }
            if ("NDL".equalsIgnoreCase(requestPayload.getIdType())) {
                customer.setDriversLicense(identity);
            }
            if ("NIN".equalsIgnoreCase(requestPayload.getIdType())) {
                customer.setNin(identity);
            }

            //Update customer record
            loanRepository.updateCustomer(customer);

            //Calculate the monthly repayment
            String monthlyRepayment = genericService.loanRepayment(new BigDecimal(requestPayload.getLoanAmount()),
                    new BigDecimal(loanType.getInterestRate()), Integer.valueOf(requestPayload.getLoanTenor()));

            Loan newLoan = new Loan();
            newLoan.setAppUser(appUser);
            newLoan.setCreatedAt(LocalDateTime.now());
            newLoan.setCustomer(customer);
            newLoan.setCustomerBusiness(requestPayload.getCustomerBusiness());
            newLoan.setDisbursedAt(LocalDate.parse("1900-01-01"));
            newLoan.setDisbursementAccount(requestPayload.getDisbursementAccount());
            newLoan.setFirstRepaymentDate(LocalDate.now());
            newLoan.setInterestRate(BigDecimal.ZERO);
            newLoan.setLiquidatedAt(LocalDate.parse("1900-01-01"));
            newLoan.setLoanAmountApproved(new BigDecimal(requestPayload.getLoanAmount()));
            newLoan.setLoanAmountRequested(new BigDecimal(requestPayload.getLoanAmount()));
            newLoan.setLoanDisbursementId("");
            String loanId = genericService.generateTransRef("LD");
            newLoan.setLoanId(loanId);
            newLoan.setLoanPurpose(requestPayload.getLoanPurpose());
            newLoan.setLoanSetup(loanType);
            newLoan.setLoanTenor(requestPayload.getLoanTenor());
            newLoan.setMaturedAt(LocalDate.parse("1900-01-01"));
            newLoan.setMobileNumber(requestPayload.getMobileNumber());
            newLoan.setMonthlyRepayment(new BigDecimal(monthlyRepayment));
            newLoan.setRequestId(requestPayload.getRequestId());
            newLoan.setStatus("Pending");
            newLoan.setTotalRepayment(BigDecimal.ZERO);
            newLoan.setTimePeriod(genericService.getTimePeriod());
            newLoan.setSelectionScore("0");
            newLoan.setSelectionScoreRating("0");
            newLoan.setLimitRange("0");
            newLoan.setMsmeScore("0");
            loanRepository.createLoan(newLoan);

            //Log the error. 
            genericService.generateLog("Artisan Loan Booking", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(disbursementAccount.getAccountNumber(), "Artisan Loan Booking", requestPayload.getLoanAmount(), channel, "Success", requestPayload.getMobileNumber(), 'S');

            LoanResponsePayload loanResponse = new LoanResponsePayload();
            loanResponse.setCustomerName(customer.getLastName() + ", " + customer.getOtherName());
            loanResponse.setFirstRepaymentDate(LocalDate.now().toString());
            loanResponse.setInterestRate(loanType.getInterestRate() + "%");
            loanResponse.setLoanAmountRequested(genericService.formatAmountWithComma(requestPayload.getLoanAmount()));
            loanResponse.setLoanAmountApproved(genericService.formatAmountWithComma(requestPayload.getLoanAmount()));
            loanResponse.setLoanId(loanId);
            loanResponse.setLoanTenor(requestPayload.getLoanTenor());
            loanResponse.setLoanType(loanType.getLoanName());
            loanResponse.setMonthlyRepayment(genericService.formatAmountWithComma(monthlyRepayment));
            loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            return gson.toJson(loanResponse);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Artisan Loan Booking", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @SuppressWarnings("unused")
    public String artisanLoanBookingFallback(String token, ArtisanLoanRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateArtisanLoanDisbursementPayload(String token, LoanIdRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "artisanLoanDisbursementFallback")
    public String processArtisanLoanDisbursement(String token, LoanIdRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Artisan Loan Disbursement", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Disbursement", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH), "API Error", "DEBUG", "");

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the loan has been disbursed already
            if (!"AUTHORIZED".equalsIgnoreCase(loanRecord.getStatus())) {
                //Log the error
                genericService.generateLog("Artisan Loan Disbursement", token, messageSource.getMessage("appMessages.loan.disbursement.failed", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Error", "DEBUG", "");

                errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.disbursement.failed", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Customer customer = loanRepository.getCustomerUsingMobileNumber(loanRecord.getCustomer().getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Disbursement", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Artisan Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the customer number
            String customerNumber = customer.getCustomerNumber().trim();
            //Check the status of the customer
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                //Log the error
                genericService.generateLog("Artisan Loan Disbursement", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Artisan Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the loan is for the customer
            if (!loanRecord.getCustomer().getMobileNumber().equalsIgnoreCase(customer.getMobileNumber())) {
                //Log the error
                genericService.generateLog("Artisan Loan Disbursement", token, messageSource.getMessage("appMessages.loan.customer.mismatch", new Object[]{requestPayload.getLoanId(), loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Artisan Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.loan.customer.mismatch", new Object[]{requestPayload.getLoanId(), loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.LOAN_CUSTOMER_MISMATCH.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.customer.mismatch", new Object[]{requestPayload.getLoanId(), loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Disbursement", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Artisan Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Call the disbursement API
            DisburseLoanRequestPayload disburse = new DisburseLoanRequestPayload();
            disburse.setAmount(loanRecord.getLoanAmountApproved().toString());
            disburse.setBranchCode(digitalBranchCode);
            disburse.setCategory(artisanLoanCode);
            disburse.setCurrency("NGN");
            disburse.setCustomerId(loanRecord.getCustomer().getCustomerNumber());
            disburse.setDrawDownAccount(loanRecord.getDisbursementAccount());
            disburse.setFrequency(String.valueOf(loanRecord.getLoanTenor()));
            disburse.setInterestRate(String.valueOf(loanRecord.getLoanSetup().getInterestRate()));
            LocalDate valueDate = LocalDate.now();
            LocalDate maturityDate = valueDate.plusMonths(Long.valueOf(loanRecord.getLoanTenor()));
            disburse.setMaturityDate(maturityDate.toString().replace("-", ""));
            disburse.setValueDate(valueDate.toString().replace("-", ""));
            String ofsRequest = gson.toJson(disburse);

            //Generate the OFS Response log
            genericService.generateLog("Artisan Loan Disbursement", token, ofsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToMiddleware("/loan/disburseDigitalLoan", ofsRequest);

            //Generate the OFS Response log
            genericService.generateLog("Artisan Loan Disbursement", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            String validationResponse = genericService.validateT24Response(middlewareResponse);
            if (validationResponse != null) {
                //Log the response
                genericService.generateLog("Artisan Loan Disbursement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                return gson.toJson(errorResponse);
            }

            DisburseLoanResponsePayload disbursePayload = gson.fromJson(middlewareResponse, DisburseLoanResponsePayload.class);
            if (!disbursePayload.getResponseCode().equalsIgnoreCase("00")) {
                //Log the response
                genericService.generateLog("Artisan Loan Disbursement", token, disbursePayload.getResponseDescription(), "API Error", "DEBUG", requestPayload.getRequestId());

                loanRecord.setStatus("FAILED");
                loanRepository.updateLoan(loanRecord);
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(disbursePayload.getResponseDescription());
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
            genericService.sendArtisanLoanOfferEmail(emailPayload);

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
            genericService.generateLog("Artisan Loan Disbursement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @SuppressWarnings("unused")
    public String artisanLoanDisbursementFallback(String token, LoanIdRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public String getPendingLoan(String token) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        genericService.generateLog("Artisan Loan Pending", token, "NA", "API Request", "INFO", "NA");
        //Check the channel information
        AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
        if (appUser == null) {
            //Log the error
            genericService.generateLog("Artisan Loan Pending", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", "");
            //Create User Activity log
            genericService.createUserActivity("", "Artisan Loan Pending", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
            return gson.toJson(errorResponse);
        }

        List<LoanResponsePayload> pendingLoans = new ArrayList<>();
        List<Loan> loanList = loanRepository.getPendingArtisanLoan();
        if (loanList != null) {
            loanList.stream().map(l -> {
                LoanResponsePayload loanResponse = new LoanResponsePayload();
                loanResponse.setCustomerName(l.getCustomer().getLastName() + ", " + l.getCustomer().getOtherName());
                loanResponse.setFirstRepaymentDate(l.getFirstRepaymentDate().toString());
                loanResponse.setInterestRate(String.valueOf(l.getLoanSetup().getInterestRate()));
                loanResponse.setLoanAmountApproved(l.getLoanAmountApproved().toString());
                loanResponse.setLoanAmountRequested(l.getLoanAmountRequested().toString());
                loanResponse.setLoanDisbursementId(l.getLoanDisbursementId());
                loanResponse.setLoanId(l.getLoanId());
                loanResponse.setLoanTenor(l.getLoanTenor());
                loanResponse.setLoanType(l.getLoanSetup().getLoanCategory());
                loanResponse.setMonthlyRepayment(l.getMonthlyRepayment().toString());
                return loanResponse;
            }).forEachOrdered(loanResponse -> {
                pendingLoans.add(loanResponse);
            });

            PendingArtisanLoanPayload pendingResponse = new PendingArtisanLoanPayload();
            pendingResponse.setPendingLoans(pendingLoans);
            pendingResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());

            return gson.toJson(pendingResponse);
        }

        //Log the error
        genericService.generateLog("Artisan Loan Pending", token, messageSource.getMessage("appMessages.loan.record.pending.noexist", new Object[]{"Artisan"}, Locale.ENGLISH), "API Response", "INFO", "");

        errorResponse = new OmniResponsePayload();
        errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
        errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.pending.noexist", new Object[]{"Artisan"}, Locale.ENGLISH));
        return gson.toJson(errorResponse);
    }

    @Override
    public boolean validateArtisanLoanAuthorizationPayload(String token, LoanIdRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "artisanLoanAuthorizationFallback")
    public String processArtisanLoanAuthorization(String token, LoanIdRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Artisan Loan Authorization", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Authorization", token, messageSource.getMessage("appMessages.loan.disbursement.failed", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", "");

                responsePayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the loan has been disbursed already
            if (!"PENDING".equalsIgnoreCase(loanRecord.getStatus())) {
                //Log the error
                genericService.generateLog("Artisan Loan Authorization", token, messageSource.getMessage("appMessages.loan.authorization.failed", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Error", "DEBUG", "");

                responsePayload.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.loan.authorization.failed", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Artisan Loan Authorization", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Artisan Loan Authorization", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            loanRecord.setStatus("AUTHORIZED");
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
            return gson.toJson(loanResponse);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Artisan Loan Authorization", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @SuppressWarnings("unused")
    public String artisanLoanAuthorizationFallback(String token, LoanIdRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    @HystrixCommand(fallbackMethod = "artisanLoanRenewalFallback")
    public String processArtisanLoanRenewal(String token, LoanRenewalRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Artisan Loan Renewal", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            List<Loan> allArtisanLoans = loanRepository.getActiveArtisanLoans();
            if (allArtisanLoans != null) {
                for (Loan loan : allArtisanLoans) {
                    String ofsRequest = loanVersion.trim() + "," + userCredentials
                            + "/," + loan.getLoanDisbursementId();

                    String middlewareResponse = genericService.postToMiddleware("/payment/postofs", ofsRequest);
                    //Generate the OFS Response log
                    genericService.generateLog("Artisan Loan Renewal", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());

                    String validationResponse = genericService.validateT24Response(middlewareResponse);
                    if (validationResponse == null) {
                        String loanStatus = genericService.getTextFromOFSResponse(middlewareResponse, "STATUS:1:1");
                        String loanAmount = genericService.getTextFromOFSResponse(middlewareResponse, "AMOUNT:1:1");
                        if (loanStatus.equalsIgnoreCase("LIQ") && loanAmount.equalsIgnoreCase("0")) {
                            String pdOfsRequest = "PD,/S/PROCESS//0" + "," + userCredentials
                                    + "/," + loan.getLoanDisbursementId();

                            //Generate the OFS Response log
                            genericService.generateLog("Artisan Loan Renewal", token, pdOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                            middlewareResponse = genericService.postToMiddleware("/payment/postofs", pdOfsRequest);
                            //Generate the OFS Response log
                            genericService.generateLog("Artisan Loan Renewal", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());

                            //Get the Total Amount field
                            String totalLoanAmount = genericService.getTextFromOFSResponse(middlewareResponse, "TOTAL.AMT.TO.REPAY:1:1");
                            if (totalLoanAmount == null || totalLoanAmount.equalsIgnoreCase("0")) {
                                //Update the loan status
                                loan.setStatus("LIQUIDATE");
                                loanRepository.updateLoan(loan);

                                //Determine the new loan amount
                                BigDecimal newAmount = BigDecimal.ZERO;
                                if (loan.getLoanAmountApproved().compareTo(new BigDecimal(rollover100Percent)) < 0) {
                                    //new amount is 100% added to the old amount
                                    newAmount = loan.getLoanAmountApproved().add(loan.getLoanAmountApproved());
                                } else {
                                    //New amount is 50% added to the old amount
                                    newAmount = loan.getLoanAmountApproved().add((loan.getLoanAmountApproved().multiply(new BigDecimal(0.5))));
                                }

                                //Check if the new amount is not greater than the maximum amount
                                BigDecimal maximumLoanAmount = loan.getLoanSetup().getMaxAmount();
                                if (newAmount.compareTo(maximumLoanAmount) <= 0) {
                                    //Calculate the monthly repayment
                                    String monthlyRepayment = genericService.loanRepayment(newAmount, loan.getInterestRate(),
                                            Integer.valueOf(loan.getLoanTenor()));

                                    //Profile the customer for another loan
                                    Loan newLoan = new Loan();
                                    newLoan.setAppUser(loan.getAppUser());
                                    newLoan.setCreatedAt(LocalDateTime.now());
                                    newLoan.setCustomer(loan.getCustomer());
                                    newLoan.setCustomerBusiness(loan.getCustomerBusiness());
                                    newLoan.setDisbursedAt(LocalDate.now());
                                    newLoan.setDisbursementAccount(loan.getDisbursementAccount());
                                    newLoan.setFirstRepaymentDate(LocalDate.now());
                                    newLoan.setInterestRate(loan.getInterestRate());
                                    newLoan.setLimitRange(loan.getLimitRange());
                                    newLoan.setLiquidatedAt(LocalDate.now());
                                    newLoan.setLoanAmountApproved(newAmount);
                                    newLoan.setLoanAmountRequested(newAmount);
                                    newLoan.setLoanDisbursementId("");
                                    newLoan.setLoanId(genericService.generateTransRef("LD"));
                                    newLoan.setLoanPurpose(loan.getLoanPurpose());
                                    newLoan.setLoanPurpose(loan.getLoanPurpose());
                                    newLoan.setLoanSetup(loan.getLoanSetup());
                                    newLoan.setLoanTenor(loan.getLoanTenor());
                                    newLoan.setMaturedAt(LocalDate.now());
                                    newLoan.setMobileNumber(loan.getMobileNumber());
                                    newLoan.setMonthlyRepayment(new BigDecimal(monthlyRepayment));
                                    newLoan.setMsmeScore(loan.getMsmeScore());
                                    newLoan.setRequestId(genericService.generateRequestId());
                                    newLoan.setSelectionScore(loan.getSelectionScore());
                                    newLoan.setSelectionScoreRating(loan.getSelectionScoreRating());
                                    newLoan.setStatus("PENDING");
                                    newLoan.setTimePeriod(genericService.getTimePeriod());
                                    newLoan.setTotalRepayment(BigDecimal.ZERO);
                                    loanRepository.createLoan(newLoan);

                                    //Successful
                                    genericService.generateLog("Artisan Loan Renewal", token, messageSource.getMessage("appMessages.record.artisan.loan.renewed", new Object[]{loan.getLoanSetup().getLoanName(), loan.getCustomer().getLastName() + ", " + loan.getCustomer().getOtherName(), newAmount}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                                    return response;
                                }
                            }
                        }
                    }
                }
            }

            //Log the response
            genericService.generateLog("Artisan Loan Renewal", token, messageSource.getMessage("appMessages.record.artisan.loan.empty", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            errorResponse.setResponseMessage(messageSource.getMessage("appMessages.record.artisan.loan.empty", new Object[0], Locale.ENGLISH));
            return gson.toJson(errorResponse);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Artisan Loan Renewal", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @SuppressWarnings("unused")
    public String artisanLoanRenewalFallback(String token, LoanRenewalRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.callback", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

}
