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
public class IdentityResponsePayload {

    private String lastName;
    private String firstName;
    private String middleName;
    private String idType;
    private String idNumber;
    private String expiryDate;
    private String gender;
    private String responseCode;
}
