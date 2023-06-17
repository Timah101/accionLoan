package com.accionmfb.omnix.loan.payload;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author dofoleta
 */
@Setter
@Getter
public class LoanBalanceResponsePayload {
     private LoanItemPayload[] loanItemPayload;
     private String ResponseCode;
}
