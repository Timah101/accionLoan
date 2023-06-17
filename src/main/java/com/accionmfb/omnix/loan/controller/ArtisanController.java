/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.controller;

import static com.accionmfb.omnix.loan.constant.ApiPaths.ARTISAN_LOAN_AUTHORIZATION;
import static com.accionmfb.omnix.loan.constant.ApiPaths.ARTISAN_LOAN_BOOKING;
import static com.accionmfb.omnix.loan.constant.ApiPaths.ARTISAN_LOAN_DISBURSEMENT;
import static com.accionmfb.omnix.loan.constant.ApiPaths.ARTISAN_LOAN_RENEWAL;
import static com.accionmfb.omnix.loan.constant.ApiPaths.ARTISAN_PENDING_LOAN;
import static com.accionmfb.omnix.loan.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.loan.constant.ApiPaths.TOKEN_PREFIX;
import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.exception.ExceptionResponse;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.payload.ArtisanLoanRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanIdRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanRenewalRequestPayload;
import com.accionmfb.omnix.loan.service.ArtisanService;
import com.accionmfb.omnix.loan.service.LoanService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "Origination", description = "Loan REST API")
@RefreshScope
public class ArtisanController {

    @Autowired
    LoanService loanService;
    @Autowired
    ArtisanService artisanService;
    @Value("${omnix.loan.environment}")
    private String loanEnvironment;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;

    @PostMapping(value = ARTISAN_LOAN_BOOKING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Booking")
    public ResponseEntity<Object> artisanLoanBooking(@Valid @RequestBody ArtisanLoanRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "BOOK_ARTISAN_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = artisanService.validateArtisanLoanBookingPayload(token, requestPayload);
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
                    String response = artisanService.processArtisanLoanBooking(token, requestPayload);
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

    @PostMapping(value = ARTISAN_PENDING_LOAN, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Pending Loan")
    public ResponseEntity<Object> pendingLoans(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "PENDING_ARTISAN_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        String response = artisanService.getPendingLoan(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = ARTISAN_LOAN_DISBURSEMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Disbursement")
    public ResponseEntity<Object> artisanLoanDisbursement(@Valid @RequestBody LoanIdRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "DISBURSE_ARTISAN_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = artisanService.validateArtisanLoanDisbursementPayload(token, requestPayload);
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
                    String response = artisanService.processArtisanLoanDisbursement(token, requestPayload);
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

    @PostMapping(value = ARTISAN_LOAN_AUTHORIZATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Disbursement")
    public ResponseEntity<Object> artisanLoanAthurization(@Valid @RequestBody LoanIdRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "AUTH_ARTISAN_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = artisanService.validateArtisanLoanAuthorizationPayload(token, requestPayload);
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
                    String response = artisanService.processArtisanLoanAuthorization(token, requestPayload);
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

    @PostMapping(value = ARTISAN_LOAN_RENEWAL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Renewal")
    public ResponseEntity<Object> artisanLoanRenewal(@Valid @RequestBody LoanRenewalRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RENEW_ARTISAN_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if (loanEnvironment.trim().equalsIgnoreCase("Production")) {
            //Valid request
            String response = artisanService.processArtisanLoanRenewal(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            /* This is for test purpose */
            String response = loanService.loanRenewalTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

}
