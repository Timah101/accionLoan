/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

/**
 *
 * @author bokon
 */
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CRCDataUploadGuarantorPayload {

    private String CustomersAccountNo;
    private String GuaranteeStatusOfLoan;
    private String TypeOfGuarantee;
    private String NameOfCorporateGuarantor;
    private String BizID;
    private String IndividualGuarantorsSurname;
    private String IndividualGuarantorsFirstName;
    private String IndividualGuarntorsMiddleName;
    private String GuarantorsDateOfBirth;
    private String GuarantorsGender;
    private String GuarantorsNationalID;
    private String GuarnatorsIntlPassportNumber;
    private String GuarantorsDriversLicenceNumber;
    private String GuarantorsBVN;
    private String GuarantorsOtherID;
    private String GuarantorsPrimaryAddressLine1;
    private String GuarantorsPrimaryAddressLine2;
    private String GuarantorsPrimaryAddressCity;
    private String GuarantorsPrimaryState;
    private String GuarantorsPrimaryCountry;
    private String GuarantorsPrimaryPhonenumber;
    private String GuarantorsEmailAddress;
}
