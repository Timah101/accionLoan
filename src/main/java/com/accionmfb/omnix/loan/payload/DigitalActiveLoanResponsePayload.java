package com.accionmfb.omnix.loan.payload;

import lombok.Data;

import java.util.List;

@Data
public class DigitalActiveLoanResponsePayload {

    private String responseCode;
    private String responseMessage;
    private boolean existingLoan;
    private List<ScheduleResponsePayloadList> scheduleList;
    private List<DigitalLoanHistoryResponseList> loanDetails;
}
