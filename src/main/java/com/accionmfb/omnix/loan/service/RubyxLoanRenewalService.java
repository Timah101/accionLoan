/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.payload.CustomerNumberRequestPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanDisbursementPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalApplyPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalQueryPayload;
import com.accionmfb.omnix.loan.payload.RubyxLoanRenewalUpdatePayload;

/**
 *
 * @author bokon
 */
public interface RubyxLoanRenewalService {

    boolean validateCustomerNumberPayload(String token, CustomerNumberRequestPayload requestPayload);

    Object rubyxLoanRenewalCheckIfSameRequestId(String requestId);

    boolean validateRubyxLoanRenewalPayload(String token, RubyxLoanRenewalPayload requestPayload);

    String processRubyxLoanRenewal(String token, RubyxLoanRenewalPayload requestPayload);

    boolean validateRubyxLoanRenewalUpdatePayload(String token, RubyxLoanRenewalUpdatePayload requestPayload);

    String processRubyxLoanRenewalUpdate(String token, RubyxLoanRenewalUpdatePayload requestPayload);

    String processRubyxLoanRenewalFetch(String token, CustomerNumberRequestPayload requestPayload);

    boolean validateRubyxLoanRenewalApplyPayload(String token, RubyxLoanRenewalApplyPayload requestPayload);

    String processRubyxLoanRenewalApply(String token, RubyxLoanRenewalApplyPayload requestPayload);

    boolean validateRubyxLoanRenewalQueryPayload(String token, RubyxLoanRenewalQueryPayload requestPayload);

    String processRubyxLoanRenewalList(String token, RubyxLoanRenewalQueryPayload requestPayload);
    
    boolean validateRubyxLoanRenewalDisbursementPayload(String token, RubyxLoanDisbursementPayload requestPayload);
    
    String disburseRubyxLoanRenewal(String token, RubyxLoanDisbursementPayload requestPayload);
}
