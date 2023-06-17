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
 * @author ofasina
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RubyxLoanRenewalAlertPayload {

    private String Alert_ID;
    private String Alert_Type;
    private String Creation_Date;
    private String Customer_ID;
    private String Amount;
    private String Product_Code;
    private String Product_Name;
    private String Eligibility_Start_Date;
    private String Eligibility_End_Date;
    private String Reference_Loan_ID;
    private String PortfolioManager_ID;
    private String Branch_Code;
    private String Score;
    private String Rating;
    private String Loan_Product_Code;

}
