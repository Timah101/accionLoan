package com.accionmfb.omnix.loan.payload;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LockAmountDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String amount;
    private String accountNumber;
    private String loanId;
    private String requestId;
}
