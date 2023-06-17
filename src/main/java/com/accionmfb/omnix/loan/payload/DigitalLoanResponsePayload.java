/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.accionmfb.omnix.loan.payload;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author dakinkuolie
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DigitalLoanResponsePayload {
    private String responseCode;
    private String loanId;
    private List<DigitalLoanHistoryResponseList> loanOffers;
    private String loanAmountRequested;
}
