/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.accionmfb.omnix.loan.payload;

import com.accionmfb.omnix.loan.model.Schedule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 *
 * @author dakinkuolie
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DigitalLoanHistoryResponseList {
    private String responseCode;
    private String responseMessage;
    private String disbursedAt;
    private String amountApproved;
    private String status;
    private String accountNumber;
    private String loanDisbursementId;
    private String loanId;
    private String bookingDate;
    private String maturityDate;
    private String narration;
    private String balance;
    private String loanAmount;
    private String pastDueAmount;
    private String repaymentAmount;
    private String repaymentDate;
    private String scheduleStatus;
    private String currentMonth;
    private String outstandingBalance;
    private PaystackCardDetails cardDetails;
    private List<ScheduleResponsePayloadList> scheduleList;


    //private List <DigitalLoanHistoryList> loanHistoryList;
}
