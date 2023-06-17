package com.accionmfb.omnix.loan.controller;

import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.exception.ExceptionResponse;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.payload.*;
import com.accionmfb.omnix.loan.security.AesService;
import com.accionmfb.omnix.loan.security.LogService;
import com.accionmfb.omnix.loan.security.PgpService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.accionmfb.omnix.loan.constant.ApiPaths.*;

/**
 *
 * @author dakinkuolie
 */
@RestController
@RequestMapping(value = "/proxy")
@Tag(name = "DigitalProxyLoan", description = "Digital Loan Proxy REST API")
@RefreshScope
public class DigitalProxyController {

    @Autowired
    LoanService loanService;
    @Autowired
    DigitalService digitalService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    LogService logService;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    PgpService pgpService;
    @Autowired
    AesService aesService;

    @Value("${security.pgp.encryption.publicKey}")
    private String recipientPublicKeyFile;

    @Value("${security.aes.encryption.key}")
    private String aesEncryptionKey;

    @Value("${security.option}")
    private String securityOption;

    private ValidationPayload validateChannelAndRequest(String role, GenericPayload requestPayload, String token) {
        ExceptionResponse exResponse = new ExceptionResponse();
        boolean userHasRole = jwtToken.userHasRole(token, role);
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));
            String exceptionJson = gson.toJson(exResponse);
            logService.logInfo("Create Individual CustomerW ith Bvn", token, messageSource.getMessage("appMessages.user.hasnorole", new Object[]{0}, Locale.ENGLISH), "API Response", exceptionJson);
            ValidationPayload validatorPayload = new ValidationPayload();
            if (securityOption.equalsIgnoreCase("AES")) {
                validatorPayload.setResponse(aesService.encryptFlutterString(exceptionJson, aesEncryptionKey));
            } else {
                validatorPayload.setResponse(pgpService.encryptString(exceptionJson, recipientPublicKeyFile));
            }
        }
        if (securityOption.equalsIgnoreCase("AES")) {
            return aesService.validateRequest(requestPayload);
        }
        return pgpService.validateRequest(requestPayload);
    }

    /*
    Authenticate IMEI before processing request for new customers
    Capture the IMEI for existing customer at login - do this one time for each customer.
     */
    @Operation(summary = "Artisan Loan Acceptance")
    @PostMapping(value = DIGITAL_LOAN_ACCEPTANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> digitalLoanAcceptance(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCEPT_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            DigitalLoanAcceptanceRequestPayload oDigitalLoanAcceptanceRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalLoanAcceptanceRequestPayload.class);
            String response = digitalService.processDigitalLoanAcceptance(token, oDigitalLoanAcceptanceRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "Digital Loan Booking")
    @PostMapping(value = DIGITAL_LOAN_BOOKING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> digitalLoanBooking(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            DigitalLoanRequestPayload oDigitalLoanRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalLoanRequestPayload.class);
            String response = digitalService.processDigitalLoanBooking(token, oDigitalLoanRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

    }

    // decline loan booking
    @Operation(summary = "Digital Loan decline")
    @PostMapping(value = DIGITAL_LOAN_DECLINE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> digitalLoanDecline(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            DigitalLoanDeclineRequestPayload oDigitalLoanDeclineRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalLoanDeclineRequestPayload.class);
            String response = digitalService.processDigitalLoanDecline(token, oDigitalLoanDeclineRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

    }

    @Operation(summary = "Digital Loan History")
    @PostMapping(value = DIGITAL_LOAN_HISTORY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> digitalLoanHistory(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            DigitalLoanHistoryRequestPayload oDigitalLoanHistoryRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalLoanHistoryRequestPayload.class);
            String response = digitalService.processDigitalLoanHistory(token, oDigitalLoanHistoryRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = DIGITAL_LOAN_ACTIVE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Digital Active Loan")
    public ResponseEntity<Object> digitalActiveLoan(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
//            DigitalLoanHistoryRequestPayload oDigitalActiveLoanRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalLoanHistoryRequestPayload.class);
            DigitalActiveLoanRequestPayload oDigitalActiveLoanRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalActiveLoanRequestPayload.class);
            String response = digitalService.processDigitalActiveLoan(token, oDigitalActiveLoanRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = DIGITAL_LOAN_PENDING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Digital Pending Loan")
    public ResponseEntity<Object> digitalPendingLoan(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
//            DigitalLoanHistoryRequestPayload oDigitalActiveLoanRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalLoanHistoryRequestPayload.class);
            DigitalActiveLoanRequestPayload digitalPendingLoan = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DigitalActiveLoanRequestPayload.class);
            String response = digitalService.processPendingLoanWithMobileNumber(token, digitalPendingLoan);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = DIGITAL_LOAN_RENEWAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Artisan Loan Renewal")
    public ResponseEntity<Object> digitalLoanRenewal(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            LoanRenewalRequestPayload oLoanRenewalRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), LoanRenewalRequestPayload.class);
            String response = digitalService.processDigitalLoanRenewal(token, oLoanRenewalRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = GUARANTOR_UPDATE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> loanGuarantor(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            GuarantorUpdatePayload oGuarantorUpdatePayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), GuarantorUpdatePayload.class);
            String response = digitalService.processGuarantorUpdate(token, oGuarantorUpdatePayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = DIGITAL_LOAN_EARLY_REPAYMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> digitalLoanEarlyRepayment(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) throws UnirestException, ParseException {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("BOOK_DIGITAL_LOAN", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            EarlyRepaymentRequestPayload earlyRepaymentRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), EarlyRepaymentRequestPayload.class);
            String response = digitalService.processEarlyRepayment(token, earlyRepaymentRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }
}
