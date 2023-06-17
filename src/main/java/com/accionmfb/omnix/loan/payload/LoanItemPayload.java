package com.accionmfb.omnix.loan.payload;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author dofoleta
 */
@Setter
@Getter
public class LoanItemPayload {

    private String accountNumber;
    private String loanId;
    private String bookingDate;
    private String maturityDate;
    private String narration;
    private String balance;
    private String loanAmount;
    private String pastDueAmount;
    private String status;
}
