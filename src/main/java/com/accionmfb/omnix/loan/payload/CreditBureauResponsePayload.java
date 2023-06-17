/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import java.util.List;
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
public class CreditBureauResponsePayload {

    private String responseCode;
    private String lastName;
    private String firstName;
    private String middleName;
    private String bvn;
    private String searchType;
    List<CreditBureauPayload> address;
    List<CreditBureauPayload> classification;
    List<CreditBureauPayload> contact;
    List<CreditBureauPayload> creditFacilityHistory;
    List<CreditBureauPayload> creditRisk;
    List<CreditBureauPayload> identification;
    List<CreditBureauPayload> inquiryProduct;
    List<CreditBureauPayload> inquiryHistory;
    List<CreditBureauPayload> performanceSummary;
}
