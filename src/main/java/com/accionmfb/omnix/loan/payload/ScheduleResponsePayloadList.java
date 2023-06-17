package com.accionmfb.omnix.loan.payload;

import lombok.Data;

import javax.persistence.Column;

@Data
public class ScheduleResponsePayloadList {
    @Column(name = "repayment_amount")
    private String repaymentAmount;
    @Column(name = "due_date")
    private String dueDate;
    @Column(name = "past_due_date")
    private String pastDueDate;
    @Column(name = "past_due_amount")
    private String pastDueAmount = "0";
    @Column(name = "early_repayment_amount")
    private String earlyRepaymentAmount = "0";
    @Column(name = "loan_id")
    private String loanId;
    @Column(name = "loan_options_id")
    private String loanOptionsId;
    private String responseCode;
    private String responseMessage;
    private String loanBalance;
    private String month;
    private String status;
    private String repaymentDate;
    private String outstandingBalance;
}
