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
 * @author dakinkuolie
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicantDetailsResponsePayload {
     private String responseCode;
    private String responseDescription;
}
