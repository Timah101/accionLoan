package com.accionmfb.omnix.loan.payload;

import lombok.Data;

@Data
public class LoanDetailsResponsePayload {

    private String mobileNumber;
    private String guarantorMobile1;
    private String guarantorName1;
    private String guarantorMobile2;
    private String guarantorName2;
    private String nameOfSpouse;
    private String noOfDependent;
    private String maritalStatus;
    private String businessName;
    private String businessType;
    private String businessAddress;
    private String typeOfResidence;
    private String rentPerYear;
    private String yearOfResidency;
    private String repaymentCard;

}
