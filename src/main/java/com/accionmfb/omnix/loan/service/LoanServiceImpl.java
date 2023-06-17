/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.model.AppUser;
import com.accionmfb.omnix.loan.model.Customer;
import com.accionmfb.omnix.loan.model.Loan;
import com.accionmfb.omnix.loan.model.LoanSetup;
import com.accionmfb.omnix.loan.payload.LoanIdRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanOfferLetterRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanRenewalRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanResponsePayload;
import com.accionmfb.omnix.loan.payload.LoanSetupResponsePayload;
import com.accionmfb.omnix.loan.payload.LoanTerminationRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanTypeRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanTypeResponsePayload;
import com.accionmfb.omnix.loan.payload.MobileNumberRequestPayload;
import com.accionmfb.omnix.loan.payload.OmniResponsePayload;
import com.accionmfb.omnix.loan.payload.PortfolioPayload;
import com.accionmfb.omnix.loan.repository.LoanRepository;
import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringJoiner;
import javax.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    GenericService genericService;
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
    @Value("${omnix.version.loan.liquidate}")
    private String liquidateVersion;
    @Value("${omnix.t24.username}")
    private String t24Username;
    @Value("${omnix.t24.password}")
    private String t24Password;
    @Value("${omnix.t24.host}")
    private String t24HostIP;
    @Value("${omnix.t24.offerletter.path}")
    private String t24OfferLetterPath;
    //Jsch related settings 
    private JSch jsch = new JSch();
    private static Session session = null;
    private static Channel channel = null;
    private ChannelSftp sftpChannel = new ChannelSftp();
    private final String downloadKnownHostFile = "c:\\software\\.ssh\\download_known_hosts.txt";

    @Override
    public String getLoanTypes(String token) {
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        try {
            List<LoanSetup> loans = loanRepository.getLoanTypes();
            if (loans == null) {
                //Log the error
                genericService.generateLog("Loan Type", token, messageSource.getMessage("appMessages.record.loan.empty", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", "");

                responsePayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.record.loan.empty", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            List<LoanSetupResponsePayload> loanTypes = new ArrayList<>();
            loans.stream().map(l -> {
                LoanSetupResponsePayload newLoan = new LoanSetupResponsePayload();
                newLoan.setAdminFee(String.valueOf(l.getAdminFee()));
                newLoan.setInsuranceFee(String.valueOf(l.getInsuranceFee()));
                newLoan.setInterestRate(String.valueOf(l.getInterestRate()));
                newLoan.setLoanAmount(l.getMinAmount() + "-" + l.getMaxAmount());
                newLoan.setLoanCategory(l.getLoanCategory());
                newLoan.setLoanDescription(l.getLoanDescription());
                newLoan.setLoanName(l.getLoanName());
                newLoan.setLoanTenor(l.getMinTenor() + "-" + l.getMaxTenor());
                newLoan.setProcessingFee(String.valueOf(l.getProcessingFee()));
                return newLoan;
            }).forEachOrdered(newLoan -> {
                loanTypes.add(newLoan);
            });

            LoanTypeResponsePayload response = new LoanTypeResponsePayload();
            response.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response.setLoans(loanTypes);
            return gson.toJson(response);
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Loan Type", token, ex.getMessage(), "API Error", "DEBUG", "");

            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public String loanBookingTest(String token, Object requestPayload) {
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        try {
            int remainder = getRandomNumber() % 2;
            if (remainder == 0) {
                LoanResponsePayload loanResponse = new LoanResponsePayload();
                loanResponse.setCustomerName("100");
                loanResponse.setFirstRepaymentDate("2021-01-01");
                loanResponse.setInterestRate("1%");
                loanResponse.setLoanAmountRequested("100,000");
                loanResponse.setLoanAmountApproved("80,000");
                loanResponse.setLoanId("LD12JK009LOT14");
                loanResponse.setLoanDisbursementId("LD2111394555");
                loanResponse.setLoanTenor("6");
                loanResponse.setLoanType("Tie & Dye");
                loanResponse.setMonthlyRepayment("20000");
                loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                return gson.toJson(loanResponse);
            } else {
                responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.ft.failed", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
        } catch (Exception ex) {
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public String loanTerminationTest(String token, Object requestPayload) {
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        try {
            int remainder = getRandomNumber() % 2;
            if (remainder == 0) {
                LoanResponsePayload loanResponse = new LoanResponsePayload();
                loanResponse.setCustomerName("100");
                loanResponse.setFirstRepaymentDate("2021-01-01");
                loanResponse.setInterestRate("1%");
                loanResponse.setLoanAmountRequested("100,000");
                loanResponse.setLoanAmountApproved("80,000");
                loanResponse.setLoanId("LD12JK009LOT14");
                loanResponse.setLoanDisbursementId("LD2111394555");
                loanResponse.setLoanTenor("6");
                loanResponse.setLoanType("Tie & Dye");
                loanResponse.setMonthlyRepayment("20000");
                loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                return gson.toJson(loanResponse);
            } else {
                responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.ft.failed", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
        } catch (Exception ex) {
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public boolean validateLoanTypePayload(String token, LoanTypeRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getLoanName().trim());
        rawString.add(requestPayload.getLoanDescription().trim());
        rawString.add(requestPayload.getLoanCategory().trim());
        rawString.add(requestPayload.getMinTenor().trim());
        rawString.add(requestPayload.getMaxTenor().trim());
        rawString.add(requestPayload.getMinAmount().trim());
        rawString.add(requestPayload.getMaxAmount().trim());
        rawString.add(requestPayload.getInterestRate().trim());
        rawString.add(requestPayload.getAdminFee().trim());
        rawString.add(requestPayload.getInsuranceFee().trim());
        rawString.add(requestPayload.getProcessingFee().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String createLoanType(String token, LoanTypeRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        try {
            //check that loan name does not exist
            LoanSetup loan = loanRepository.getLoanTypeUsingCategory(requestPayload.getLoanName());
            if (loan != null) {
                //Log the error
                genericService.generateLog("Loan Setup", token, messageSource.getMessage("appMessages.loan.setupexist", new Object[]{requestPayload.getLoanName()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.setupexist", new Object[]{requestPayload.getLoanName()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //check that the maximum amount is not less than the minimum amount
            if (Double.valueOf(requestPayload.getMaxAmount()) <= Double.valueOf(requestPayload.getMinAmount())) {
                //Log the error
                genericService.generateLog("Loan Setup", token, messageSource.getMessage("appMessages.loan.same", new Object[]{" Loan Amount"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.SAME_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.same", new Object[]{" Loan Amoount"}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //check that the maximum tenor is not less than the minimum tenor
            if (Integer.valueOf(requestPayload.getMaxTenor()) <= Integer.valueOf(requestPayload.getMinTenor())) {
                //Log the error
                genericService.generateLog("Loan Setup", token, messageSource.getMessage("appMessages.loan.same", new Object[]{" Loan Tenor"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.SAME_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.same", new Object[]{" Loan Tenor"}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Loan Setup", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //cretate new loan setup
            LoanSetup newLoanSetup = new LoanSetup();
            newLoanSetup.setAppUser(appUser);
            newLoanSetup.setLoanCategory(requestPayload.getLoanCategory());
            newLoanSetup.setLoanDescription(requestPayload.getLoanDescription());
            newLoanSetup.setLoanName(requestPayload.getLoanName());
            newLoanSetup.setProcessingFee(Double.valueOf(requestPayload.getProcessingFee()));
            newLoanSetup.setAdminFee(Double.valueOf(requestPayload.getAdminFee()));
            newLoanSetup.setInsuranceFee(Double.valueOf(requestPayload.getInsuranceFee()));
            newLoanSetup.setInterestRate(Double.valueOf(requestPayload.getInterestRate().replace("%", "")));
            newLoanSetup.setMinTenor(Integer.valueOf(requestPayload.getMinTenor()));
            newLoanSetup.setMaxTenor(Integer.valueOf(requestPayload.getMaxTenor()));
            newLoanSetup.setMaxAmount(BigDecimal.valueOf(Double.valueOf(requestPayload.getMaxAmount())));
            newLoanSetup.setMinAmount(BigDecimal.valueOf(Double.valueOf(requestPayload.getMinAmount())));
            newLoanSetup.setStatus("ENABLED");
            LoanSetup createLoanSetup = loanRepository.createLoanSetup(newLoanSetup);

            LoanSetupResponsePayload responsePayload = new LoanSetupResponsePayload();
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            responsePayload.setAdminFee(genericService.formatAmountWithComma(String.valueOf(createLoanSetup.getAdminFee())));
            responsePayload.setInsuranceFee(genericService.formatAmountWithComma(String.valueOf(createLoanSetup.getInsuranceFee())));
            responsePayload.setInterestRate(genericService.formatAmountWithComma(String.valueOf(createLoanSetup.getInterestRate())));
            responsePayload.setLoanAmount(genericService.formatAmountWithComma(createLoanSetup.getMinAmount().toString()) + "-" + genericService.formatAmountWithComma(createLoanSetup.getMaxAmount().toString()));
            responsePayload.setLoanCategory(createLoanSetup.getLoanCategory());
            responsePayload.setLoanDescription(createLoanSetup.getLoanDescription());
            responsePayload.setLoanName(createLoanSetup.getLoanName());
            responsePayload.setLoanTenor(createLoanSetup.getMinTenor() + "-" + createLoanSetup.getMaxTenor() + " Months");
            responsePayload.setProcessingFee(genericService.formatAmountWithComma(String.valueOf(createLoanSetup.getProcessingFee())));
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    private int getRandomNumber() {
        return (int) ((Math.random() * (5 - 1)) + 1);
    }

    @Override
    public String loanDisbursementTest(String token, Object requestPayload) {
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        try {
            int remainder = getRandomNumber() % 2;
            if (remainder == 0) {
                LoanResponsePayload loanResponse = new LoanResponsePayload();
                loanResponse.setCustomerName("100");
                loanResponse.setFirstRepaymentDate("2021-01-01");
                loanResponse.setInterestRate("1%");
                loanResponse.setLoanAmountRequested("100,000");
                loanResponse.setLoanAmountApproved("80,000");
                loanResponse.setLoanId("LD12JK009LOT14");
                loanResponse.setLoanDisbursementId("LD2111394555");
                loanResponse.setLoanTenor("6");
                loanResponse.setLoanType("Tie & Dye");
                loanResponse.setMonthlyRepayment("20000");
                loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                return gson.toJson(loanResponse);
            } else {
                responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.ft.failed", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
        } catch (Exception ex) {
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public String loanRenewalTest(String token, LoanRenewalRequestPayload requestPayload) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object checkIfSameRequestId(String requestId) {
        try {
            Loan loanRecord = loanRepository.getRecordUsingRequestId(requestId);
            return loanRecord == null;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public boolean validateLoanDetailsPayload(String token, LoanIdRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processLoanDetails(String token, LoanIdRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Loan Details", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Loan Details", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH), "API Error", "DEBUG", "");

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Loan Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Loan Details", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

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
            loanResponse.setStatus(loanRecord.getStatus());
            return gson.toJson(loanResponse);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Loan Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateArtisanLoanTerminationPayload(String token, LoanTerminationRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getLoanId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processArtisanLoanTermination(String token, LoanTerminationRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Loan Termination", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check if the loan record exist
            Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Loan Termination", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Loan Termination", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the customer number
            String customerNumber = customer.getCustomerNumber().trim();
            //Check the status of the customer
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                //Log the error
                genericService.generateLog("Loan Termination", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Loan Termination", "", channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Loan Termination", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Loan Termination", "", channel, messageSource.getMessage("appMessages.loan.record.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the status of the loan request
            if (!"DISBURSED".equalsIgnoreCase(loanRecord.getStatus())) {
                //Log the error
                genericService.generateLog("Loan Termination", token, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Loan Termination", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the loan is for the customer
            if (!loanRecord.getCustomer().getMobileNumber().equalsIgnoreCase(customer.getMobileNumber())) {
                //Log the error
                genericService.generateLog("Loan Termination", token, messageSource.getMessage("appMessages.loan.customer.mismatch", new Object[]{requestPayload.getLoanId(), requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Loan Termination", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.loan.customer.mismatch", new Object[]{requestPayload.getLoanId(), requestPayload.getMobileNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.LOAN_CUSTOMER_MISMATCH.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.customer.mismatch", new Object[]{requestPayload.getLoanId(), requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Loan Termination", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(loanRecord.getDisbursementAccount(), "Loan Termination", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Initiate the OFS to liquidate the 
            String ofsRequest = liquidateVersion.trim() + "," + userCredentials + "/," + loanRecord.getLoanDisbursementId();
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Loan Termination", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Loan Termination", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());

            String validationResponse = genericService.validateT24Response(middlewareResponse);
            if (validationResponse != null) {
                //Log the response
                genericService.generateLog("Loan Termination", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                return gson.toJson(errorResponse);
            }

            loanRecord.setStatus("LIQUIDATE");
            loanRecord.setLiquidatedAt(LocalDate.now());
            Loan updateLoan = loanRepository.updateLoan(loanRecord);

            LoanResponsePayload loanResponse = new LoanResponsePayload();
            loanResponse.setCustomerName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
            loanResponse.setInterestRate(loanRecord.getLoanSetup().getInterestRate() + "%");
            loanResponse.setLoanAmountRequested(loanRecord.getLoanAmountRequested().toString());
            loanResponse.setLoanAmountApproved(loanRecord.getLoanAmountApproved().toString());
            loanResponse.setLoanId(loanRecord.getLoanId());
            loanResponse.setLoanTenor(loanRecord.getLoanTenor());
            loanResponse.setLoanType(loanRecord.getLoanSetup().getLoanName());
            loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            loanResponse.setStatus(updateLoan.getStatus());
            return gson.toJson(loanResponse);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Loan Termination", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateLoanOfferLetterPayload(String token, LoanOfferLetterRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getOfferType().trim());
        rawString.add(requestPayload.getSearchType().trim());
        rawString.add(requestPayload.getSearchId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String createLoanOfferLetter(String token, LoanOfferLetterRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Loan Offer Letter", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Loan Offer Letter", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Loan Offer Letter", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Set the customer number
            String customerNumber = customer.getCustomerNumber().trim();
            //Check the status of the customer
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                //Log the error
                genericService.generateLog("Loan Offer Letter", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Loan Offer Letter", "", channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Loan Offer Letter", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Loan Offer Letter", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the CEF or Loan ID is supplied
            if ("CEF".equalsIgnoreCase(requestPayload.getSearchType())) {
                //Check the offer letter type specified
                if ("NA".equalsIgnoreCase(requestPayload.getOfferType())) {
                    //Log the error
                    genericService.generateLog("Loan Offer Letter", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getOfferType(), requestPayload.getSearchType()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(customerNumber, "Loan Offer Letter", "", channel, messageSource.getMessage("appMessages.loan.record.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                    errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.offer.invalid", new Object[]{requestPayload.getOfferType(), requestPayload.getSearchType()}, Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }

                //Set the directory to dump the CEF
                String destinationDirectory = "C:\\Omnix\\OfferLetter\\";
                String ofsRequest = "\"ENQUIRY.SELECT,," + userCredentials;
                switch (requestPayload.getOfferType()) {
                    case "Individual":
                        ofsRequest = ofsRequest + ",AMFB.OFFERX,CEF.ID:EQ=" + requestPayload.getSearchId();
                        String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        String middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        String validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        String[] splitString = middlewareResponse.split(",");
                        String okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("INDIVIDUAL OFFER LETTER GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "INDV.OFFER." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "INDV.OFFER." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "INDV.OFFER." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "INDV.OFFER." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String stringToEncode = new String(fisBytes, StandardCharsets.US_ASCII);
                                String base64String = Base64.getEncoder().encodeToString(stringToEncode.getBytes());
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                    case "Promissory":
                        ofsRequest = ofsRequest + ",AMFB.PROMISE.NOTE.NW,CEF.ID:EQ=" + requestPayload.getSearchId();
                        newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        splitString = middlewareResponse.split(",");
                        okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("PAY-GO PROMISORY LETTER GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "PAYGO.PROM." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "PAYGO.PROM." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "PAYGO.PROM." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "PAYGO.PROM." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String base64String = Base64.getEncoder().encodeToString(fisBytes);
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                                return gson.toJson(responsePayload);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                    case "Guarantee":
                        ofsRequest = ofsRequest + ",AMFB.GUARAN,CEF.ID:EQ=" + requestPayload.getSearchId();
                        newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        splitString = middlewareResponse.split(",");
                        okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("INDIVIDUAL GUARANTOR LETTER GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "INDV.GUARAN." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "INDV.GUARAN." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "INDV.GUARAN." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "INDV.GUARAN." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String base64String = Base64.getEncoder().encodeToString(fisBytes);
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                                return gson.toJson(responsePayload);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                    case "PayGo":
                        ofsRequest = ofsRequest + ",PAYGO.OFFER,CEF.ID:EQ=" + requestPayload.getSearchId();
                        newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        splitString = middlewareResponse.split(",");
                        okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("PAY-GO OFFER LETTER GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "PAYGO.OFFER." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "PAYGO.OFFER." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "PAYGO.OFFER." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "PAYGO.OFFER." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String base64String = Base64.getEncoder().encodeToString(fisBytes);
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                                return gson.toJson(responsePayload);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                    case "PayGoAccrual":
                        ofsRequest = ofsRequest + ",AMFB.PAYGO,CEF.ID:EQ=" + requestPayload.getSearchId();
                        newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        splitString = middlewareResponse.split(",");
                        okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("PAY-GO OFFER LETTER GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "PAYGO.OFFER." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "PAYGO.OFFER." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "PAYGO.OFFER." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "PAYGO.OFFER." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String base64String = Base64.getEncoder().encodeToString(fisBytes);
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                                return gson.toJson(responsePayload);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                    case "PayGoPromissory":
                        ofsRequest = ofsRequest + ",PAYGO.PROM,CEF.ID:EQ=" + requestPayload.getSearchId();
                        newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        splitString = middlewareResponse.split(",");
                        okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("PAY-GO PROMISORY LETTER GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "PAYGO.PROM." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "PAYGO.PROM." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "PAYGO.PROM." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "PAYGO.PROM." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String base64String = Base64.getEncoder().encodeToString(fisBytes);
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                                return gson.toJson(responsePayload);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                    case "BillOfSales":
                        ofsRequest = ofsRequest + ",BILL.SALE,CEF.ID:EQ=" + requestPayload.getSearchId();
                        newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        splitString = middlewareResponse.split(",");
                        okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("BILL OF SALE GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "BILL." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "BILL." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "BILL." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "BILL." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String base64String = Base64.getEncoder().encodeToString(fisBytes);
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                                return gson.toJson(responsePayload);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                    case "School":
                        ofsRequest = ofsRequest + ",SCH.OFFER,CEF.ID:EQ=" + requestPayload.getSearchId();
                         newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Initiate call to T24
                        genericService.generateLog("Loan Offer Letter", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        middlewareResponse = genericService.postToT24(ofsRequest);

                        //Generate the OFS Response log
                        genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
                        validationResponse = genericService.validateT24Response(middlewareResponse);
                        if (validationResponse != null) {
                            //Log the response
                            genericService.generateLog("Loan Offer Letter", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());

                            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                            errorResponse.setResponseMessage(middlewareResponse);
                            return gson.toJson(errorResponse);
                        }

                        splitString = middlewareResponse.split(",");
                        okResult = splitString[3].replace("\"", "").replace("\\", "");
                        if (okResult.equals("INDIVIDUAL OFFER LETTER GENERATED")) {
                            try {
                                connectToT24();
                                String fileToCopy = t24OfferLetterPath + "INDV.OFFER." + requestPayload.getSearchId();
                                sftpChannel.get(fileToCopy, destinationDirectory, null, 0);
                                disconnectSession();
                                File file = new File(destinationDirectory + "INDV.OFFER." + requestPayload.getSearchId() + ".pdf");
                                file.getParentFile().mkdirs();
                                genericService.createPdf(destinationDirectory + "INDV.OFFER." + requestPayload.getSearchId() + ".pdf", destinationDirectory + "INDV.OFFER." + requestPayload.getSearchId());
                                FileInputStream fis = new FileInputStream(file);
                                byte[] fisBytes = fis.readAllBytes();
                                String base64String = Base64.getEncoder().encodeToString(fisBytes);
                                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                                responsePayload.setResponseMessage(base64String);
                                return gson.toJson(responsePayload);
                            } catch (SftpException | java.io.IOException ex) {
                                //Log the response
                                genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

                                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                                errorResponse.setResponseMessage(ex.getMessage());
                                return gson.toJson(errorResponse);
                            }
                        }
                        break;
                }
            }

            //Check if the loan record exist
            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getSearchId());
            if (loanRecord == null) {
                //Log the error
                genericService.generateLog("Loan Offer Letter", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getSearchId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customerNumber, "Loan Offer Letter", "", channel, messageSource.getMessage("appMessages.loan.record.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getSearchId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
            return null;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Loan Offer Letter", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

    private void connectToT24() throws FileNotFoundException {
        try {
            jsch.setKnownHosts(new FileInputStream("c:\\software\\.ssh\\download_known_hosts.txt"));
            session = jsch.getSession(t24Username, t24HostIP, 22);
            session.setPassword(t24Password);
            session.setTimeout(100000);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    private void disconnectSession() {
        if (session != null) {
            sftpChannel.disconnect();
            channel.disconnect();
            session.disconnect();
        }
    }

    @Override
    public boolean validateMobileNumberPayload(String token, MobileNumberRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String createLoanStatus(String token, MobileNumberRequestPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String requestBy = jwtToken.getUsernameFromToken(token);
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Loan Status", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = loanRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Loan Status", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Loan Status", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Loan Status", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getMobileNumber(), "Loan Status", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the customer has existing loan in the domicile branch
            Object loanBalances = genericService.getLoanBalances(customer.getBranch().getBranchCode(), customer.getCustomerNumber(), userCredentials);
            if (!(loanBalances instanceof String)) {
                List<PortfolioPayload> loanList = (List<PortfolioPayload>) loanBalances;
                //if (loanList == null) {
                if (loanList == null) {
                    //The customer has no loan
                    responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    responsePayload.setResponseMessage("Customer has no loan running");
                    //Log the error
                    genericService.generateLog("Loan Status", token, messageSource.getMessage("appMessages.loan.exist", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    return gson.toJson(responsePayload);
                }

                String loanDetails = "";
                for (PortfolioPayload l : loanList) {
                    loanDetails = "Loan with ID: " + l.getLoanId() + " has N" + l.getLoanBalance() + " balance outstanding";
                }

                //The customer has no loan
                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                responsePayload.setResponseMessage(loanDetails);
                //Log the error
                genericService.generateLog("Loan Status", token, loanDetails, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                return gson.toJson(responsePayload);
            }

            String loanResponse = (String) loanBalances;
            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            if (loanResponse.contains("NO RECORDS RETURNED BY ROUTINE BASED SELECTION")) {
                errorResponse.setResponseMessage("Customer has no loan running");
            } else {
                errorResponse.setResponseMessage(loanResponse);
            }

            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Loan Status", token, (String) loanBalances, "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Loan Status", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

}
