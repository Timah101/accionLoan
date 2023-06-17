/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BehaviouralAnalysis {

    private String gamblingRate;
    private String inflowOutFlowRate;
    private String accountSweep;
    private String loanAmount;
    private String loanInflowRate;
    private String loanRepaymentInflowRate;
    private String loanRepayments;
    private String topIncomingTransferAccount;
    private String topTransferRecipientAccount;
}
