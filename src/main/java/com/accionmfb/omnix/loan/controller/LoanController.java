/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.controller;

import static com.accionmfb.omnix.loan.constant.ApiPaths.GUARANTOR_UPDATE;
import static com.accionmfb.omnix.loan.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.loan.constant.ApiPaths.LOAN_DETAILS;
import static com.accionmfb.omnix.loan.constant.ApiPaths.LOAN_OFFER_LETTER;
import static com.accionmfb.omnix.loan.constant.ApiPaths.LOAN_SETUP;
import static com.accionmfb.omnix.loan.constant.ApiPaths.LOAN_STATUS;
import static com.accionmfb.omnix.loan.constant.ApiPaths.LOAN_TERMINATION;
import static com.accionmfb.omnix.loan.constant.ApiPaths.LOAN_TYPE_LISTING;
import static com.accionmfb.omnix.loan.constant.ApiPaths.STATISTICS_MEMORY;
import static com.accionmfb.omnix.loan.constant.ApiPaths.TOKEN_PREFIX;
import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.exception.ExceptionResponse;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.payload.LoanIdRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanOfferLetterRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanTerminationRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanTypeRequestPayload;
import com.accionmfb.omnix.loan.payload.MemoryStats;
import com.accionmfb.omnix.loan.payload.MobileNumberRequestPayload;
import com.accionmfb.omnix.loan.service.GenericService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "Loan Setup", description = "Loan REST API")
@RefreshScope
public class LoanController {

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

    @PostMapping(value = LOAN_SETUP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> loanSetup(@Valid @RequestBody LoanTypeRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();

        //Check if the IP address is from the admin consol
        String adminIPResponse = genericService.checkAdminIP(httpRequest.getRemoteAddr());
        if (adminIPResponse != null) {
            return new ResponseEntity<>(adminIPResponse, HttpStatus.OK);
        }

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "LOAN_SETUP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = loanService.validateLoanTypePayload(token, requestPayload);
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
                String response = loanService.createLoanType(token, requestPayload);
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

    @PostMapping(value = LOAN_TYPE_LISTING, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List of Loan types")
    public ResponseEntity<Object> loanTypes(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "LOAN_TYPE_LISTING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        String response = loanService.getLoanTypes(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = LOAN_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch Loan Details")
    public ResponseEntity<Object> loanDetails(@Valid @RequestBody LoanIdRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "LOAN_SETUP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = loanService.validateLoanDetailsPayload(token, requestPayload);
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
                    String response = loanService.processLoanDetails(token, requestPayload);
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

    @GetMapping(value = STATISTICS_MEMORY, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch the JVM statistics")
    public MemoryStats getMemoryStatistics(HttpServletRequest httpRequest) {
        MemoryStats stats = new MemoryStats();
        stats.setHeapSize(Runtime.getRuntime().totalMemory());
        stats.setHeapMaxSize(Runtime.getRuntime().maxMemory());
        stats.setHeapFreeSize(Runtime.getRuntime().freeMemory());
        return stats;
    }

    @PostMapping(value = LOAN_TERMINATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Loan Termination")
    public ResponseEntity<Object> artisanLoanTermination(@Valid @RequestBody LoanTerminationRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TERMINATE_LOAN");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(loanEnvironment.trim())) {
            boolean payloadValid = loanService.validateArtisanLoanTerminationPayload(token, requestPayload);
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
                    String response = loanService.processArtisanLoanTermination(token, requestPayload);
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
            String response = loanService.loanTerminationTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping(value = LOAN_OFFER_LETTER, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> loanOfferLetter(@Valid @RequestBody LoanOfferLetterRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "LOAN_SETUP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = loanService.validateLoanOfferLetterPayload(token, requestPayload);
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
                String response = loanService.createLoanOfferLetter(token, requestPayload);
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

    @PostMapping(value = LOAN_STATUS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> loanStatus(@Valid @RequestBody MobileNumberRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();

        //Check if the IP address is from the admin consol
        String adminIPResponse = genericService.checkAdminIP(httpRequest.getRemoteAddr());
        if (adminIPResponse != null) {
            return new ResponseEntity<>(adminIPResponse, HttpStatus.OK);
        }

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "LOAN_SETUP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = loanService.validateMobileNumberPayload(token, requestPayload);
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
                String response = loanService.createLoanStatus(token, requestPayload);
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
    
    


}
