/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.payload.ArtisanLoanRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanIdRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanDisbursementRequestPayload;
import com.accionmfb.omnix.loan.payload.LoanRenewalRequestPayload;

/**
 *
 * @author bokon
 */
public interface ArtisanService {

    boolean validateArtisanLoanBookingPayload(String token, ArtisanLoanRequestPayload requestPayload);

    String processArtisanLoanBooking(String token, ArtisanLoanRequestPayload requestPayload);

    boolean validateArtisanLoanDisbursementPayload(String token, LoanIdRequestPayload requestPayload);

    String processArtisanLoanDisbursement(String token, LoanIdRequestPayload requestPayload);

    String getPendingLoan(String token);

    boolean validateArtisanLoanAuthorizationPayload(String token, LoanIdRequestPayload requestPayload);

    String processArtisanLoanAuthorization(String token, LoanIdRequestPayload requestPayload);

    String processArtisanLoanRenewal(String token, LoanRenewalRequestPayload requestPayload);
  
}
