/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Data
public class FundsTransferResponsePayload {

    private String debitAccount;
    private String debitAccountName;
    private String creditAccount;
    private String creditAccountName;
    private String amount;
    private String narration;
    private String responseCode;
    private String transRef;
    private String status;
    private String handshakeId;
    private String t24TransRef; 
}
