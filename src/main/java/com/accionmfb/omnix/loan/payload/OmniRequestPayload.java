/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import lombok.Data;

/**
 *
 * @author ofasina
 */
@Data
public class OmniRequestPayload {
    
    private String mobileNumber;

    private String requestId;

    private String token;
    
    private String hash;
    
}
