/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.payload.LoanIdRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanOfferLetterRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanRenewalRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanTerminationRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanTypeRequestPayload;
import com.accionmfb.omnix.loan.payload.MobileNumberRequestPayload;

/**
 *
 * @author bokon
 */
public interface LoanService {

    String getLoanTypes(String token);

    boolean validateLoanTypePayload(String token, LoanTypeRequestPayload requestPayload);

    String createLoanType(String token, LoanTypeRequestPayload requestPayload);

    String loanBookingTest(String token, Object requestPayload);

    String loanDisbursementTest(String token, Object requestPayload);

    String loanRenewalTest(String token, LoanRenewalRequestPayload requestPayload);

    Object checkIfSameRequestId(String requestId);

    boolean validateLoanDetailsPayload(String token, LoanIdRequestPayload requestPayload);

    String processLoanDetails(String token, LoanIdRequestPayload requestPayload);

    String loanTerminationTest(String token, Object requestPayload);

    boolean validateArtisanLoanTerminationPayload(String token, LoanTerminationRequestPayload requestPayload);

    String processArtisanLoanTermination(String token, LoanTerminationRequestPayload requestPayload);

    boolean validateLoanOfferLetterPayload(String token, LoanOfferLetterRequestPayload requestPayload);

    String createLoanOfferLetter(String token, LoanOfferLetterRequestPayload requestPayload);

    boolean validateMobileNumberPayload(String token, MobileNumberRequestPayload requestPayload);

    String createLoanStatus(String token, MobileNumberRequestPayload requestPayload);
    
}
