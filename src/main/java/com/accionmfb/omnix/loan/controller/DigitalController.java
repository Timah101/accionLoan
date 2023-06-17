/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.controller;

import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.exception.ExceptionResponse;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.payload.*;
import com.accionmfb.omnix.loan.service.DigitalService;
import com.accionmfb.omnix.loan.service.LoanService;
import com.google.gson.Gson;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.text.ParseException;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.accionmfb.omnix.loan.constant.ApiPaths.*;

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "Digital Loan", description = "Loan REST API")
@RefreshScope
public class DigitalController {

    @Autowired
    LoanService loanService;
    @Autowired
    DigitalService digitalService;
    @Autowired
    MessageSource messageSource;
    @Value("${omnix.loan.environment}")
    private String loanEnvironment;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;

    @PostMapping(value = DIGITAL_LOAN_BOOKING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Booking")
    public ResponseEntity<Object> digitalLoanBooking(@Valid @RequestBody DigitalLoanRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
//            boolean payloadValid = digitalService.validateDigitalLoanBookingPayload(token, requestPayload);
            if (true) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = digitalService.processDigitalLoanBooking(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanBookingTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping(value = DIGITAL_LOAN_ACCEPTANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Acceptance")
    public ResponseEntity<Object> digitalLoanAcceptance(@Valid @RequestBody DigitalLoanAcceptanceRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCEPT_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = digitalService.validateDigitalLoanAcceptancePayload(token, requestPayload);
            if (true) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = digitalService.processDigitalLoanAcceptance(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanDisbursementTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    // decline loan booking
     @PostMapping(value = DIGITAL_LOAN_DECLINE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Digital Loan decline")
    public ResponseEntity<Object> digitalLoanDecline(@Valid @RequestBody DigitalLoanDeclineRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = digitalService.validateDigitalLoanDeclinePayload(token, requestPayload);
            if (payloadValid) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = digitalService.processDigitalLoanDecline(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanDisbursementTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }
    
    
    
    
    
     
    
    
    
    @PostMapping(value = DIGITAL_LOAN_HISTORY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Digital Loan Disbursement")
    public ResponseEntity<Object> digitalLoanHistory(@Valid @RequestBody DigitalLoanHistoryRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = digitalService.validateDigitalLoanHistoryPayload(token, requestPayload);
            if (true) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = digitalService.processDigitalLoanHistory(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanDisbursementTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }
    
    
    
     @PostMapping(value = DIGITAL_LOAN_ACTIVE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Digital Active Loan")
    public ResponseEntity<Object> digitalActiveLoan(@Valid @RequestBody DigitalActiveLoanRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
//            boolean payloadValid = digitalService.validateDigitalLoanHistoryPayload(token, requestPayload);
            if (true) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    System.out.println("The Active payload in controller " + requestPayload);
                    //Valid request
                    String response = digitalService.processDigitalActiveLoan(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanDisbursementTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping(value = DIGITAL_LOAN_PENDING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Digital Pending Loan")
    public ResponseEntity<Object> digitalPendingLoan(@Valid @RequestBody DigitalActiveLoanRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
//            boolean payloadValid = digitalService.validateDigitalLoanHistoryPayload(token, requestPayload);
            if (true) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = digitalService.processPendingLoanWithMobileNumber(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanDisbursementTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }
    


    @PostMapping(value = DIGITAL_LOAN_RENEWAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Renewal")
    public ResponseEntity<Object> digitalLoanRenewal(@Valid @RequestBody LoanRenewalRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RENEW_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = digitalService.validateDigitalLoanRenewalPayload(token, requestPayload);
            if (payloadValid) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = digitalService.processDigitalLoanRenewal(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanRenewalTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }
    
    @PostMapping(value = GUARANTOR_UPDATE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> loanGuarantor(@Valid @RequestBody GuarantorUpdatePayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));
            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        //validate the requestPayload
        boolean payloadValid = digitalService.validateGuarantorUpdatePayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = digitalService.processGuarantorUpdate(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = DIGITAL_LOAN_EARLY_REPAYMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Early Repayment")
    public ResponseEntity<Object> digitalLoanEarlyRepayment(@Valid @RequestBody EarlyRepaymentRequestPayload requestPayload, HttpServletRequest httpRequest) throws UnirestException, ParseException {

        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        ExceptionResponse exResponse = new ExceptionResponse();
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_DIGITAL_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));
            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
//            boolean payloadValid = digitalService.validateDigitalLoanHistoryPayload(token, requestPayload);
            if (true) {
                //Check if the request contains the same Request ID
                Object recordExist = loanService.checkIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = digitalService.processEarlyRepayment(token, requestPayload);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                exResponse.setResponseMessage((String) recordExist);

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = loanService.loanDisbursementTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }



    }


