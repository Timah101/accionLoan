/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.controller;

import static com.accionmfb.omnix.loan.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.loan.constant.ApiPaths.RUBYX_LOAN_RENEWAL;
import static com.accionmfb.omnix.loan.constant.ApiPaths.RUBYX_LOAN_RENEWAL_APPLY;
import static com.accionmfb.omnix.loan.constant.ApiPaths.RUBYX_LOAN_RENEWAL_DISBURSEMENT;
import static com.accionmfb.omnix.loan.constant.ApiPaths.RUBYX_LOAN_RENEWAL_FETCH;
import static com.accionmfb.omnix.loan.constant.ApiPaths.RUBYX_LOAN_RENEWAL_LIST;
import static com.accionmfb.omnix.loan.constant.ApiPaths.RUBYX_LOAN_RENEWAL_UPDATE;
import static com.accionmfb.omnix.loan.constant.ApiPaths.TOKEN_PREFIX;
import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.exception.ExceptionResponse;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.payload.CustomerNumberRequestPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanDisbursementPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalApplyPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalQueryPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalUpdatePayload;
import com.accionmfb.omnix.loan.service.GenericService;
import com.accionmfb.omnix.loan.service.LoanService;
import com.accionmfb.omnix.loan.service.RubyxLoanRenewalService;
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
public class RubyxController {

    @Autowired
    RubyxLoanRenewalService bauService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    LoanService loanService;
    @Autowired
    GenericService genericService;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    @Value("${omnix.loan.environment}")
    private String loanEnvironment;

    @PostMapping(value = RUBYX_LOAN_RENEWAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rubyx Auto Loan Renewal")
    public ResponseEntity<Object> rubyxLoanRenewal(@Valid @RequestBody RubyxLoanRenewalPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RUBYX");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = bauService.validateRubyxLoanRenewalPayload(token, requestPayload);
            if (payloadValid) {
                //Check if the request contains the same Request ID
                Object recordExist = bauService.rubyxLoanRenewalCheckIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = bauService.processRubyxLoanRenewal(token, requestPayload);
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

    @PostMapping(value = RUBYX_LOAN_RENEWAL_UPDATE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rubyx Auto Loan Renewal Update")
    public ResponseEntity<Object> rubyxLoanRenewalUpdate(@Valid @RequestBody RubyxLoanRenewalUpdatePayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RUBYX");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = bauService.validateRubyxLoanRenewalUpdatePayload(token, requestPayload);
            if (payloadValid) {
                //Check if the request contains the same Request ID
                Object recordExist = bauService.rubyxLoanRenewalCheckIfSameRequestId(requestPayload.getRequestId());
                if (recordExist instanceof Boolean) {
                    if (!(boolean) recordExist) {
                        exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                        exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                        String exceptionJson = gson.toJson(exResponse);
                        return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                    }
                    //Valid request
                    String response = bauService.processRubyxLoanRenewalUpdate(token, requestPayload);
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

    @PostMapping(value = RUBYX_LOAN_RENEWAL_APPLY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rubyx Auto Loan Renewal Application")
    public ResponseEntity<Object> rubyxLoanRenewalApply(@Valid @RequestBody RubyxLoanRenewalApplyPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RUBYX");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = bauService.validateRubyxLoanRenewalApplyPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = bauService.processRubyxLoanRenewalApply(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = RUBYX_LOAN_RENEWAL_FETCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rubyx Auto Loan Renewal Fetch")
    public ResponseEntity<Object> rubyxLoanRenewalFetch(@Valid @RequestBody CustomerNumberRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RUBYX");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = bauService.validateCustomerNumberPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = bauService.processRubyxLoanRenewalFetch(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = RUBYX_LOAN_RENEWAL_LIST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rubyx Auto Loan Renewal Fetch")
    public ResponseEntity<Object> rubyxLoanRenewalList(@Valid @RequestBody RubyxLoanRenewalQueryPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RUBYX");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = bauService.validateRubyxLoanRenewalQueryPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = bauService.processRubyxLoanRenewalList(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }
    
    @PostMapping(value = RUBYX_LOAN_RENEWAL_DISBURSEMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rubyx Auto Loan Renewal Application")
    public ResponseEntity<Object> rubyxLoanRenewalDisbursement(@Valid @RequestBody RubyxLoanDisbursementPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "RUBYX");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = bauService.validateRubyxLoanRenewalDisbursementPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = bauService.disburseRubyxLoanRenewal(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

}
