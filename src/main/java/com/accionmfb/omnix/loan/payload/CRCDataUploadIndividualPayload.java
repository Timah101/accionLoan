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
public class CRCDataUploadIndividualPayload {

    private String CustomerID;
    private String BranchCode;
    private String Surname;
    private String Firstname;
    private String Middlename;
    private String DateofBirth;
    private String NationalIdentityNumber;
    private String DriversLicenseNo;
    private String BVNNo;
    private String PassportNo;
    private String Gender;
    private String Nationality;
    private String MaritalStatus;
    private String Mobilenumber;
    private String PrimaryAddressLine1;
    private String PrimaryAddressLine2;
    private String Primarycity;
    private String PrimaryState;
    private String PrimaryCountry;
    private String EmploymentStatus;
    private String Occupation;
    private String BusinessCategory;
    private String BusinessSector;
    private String BorrowerType;
    private String OtherID;
    private String TaxID;
    private String PictureFilePath;
    private String Emailaddress;
    private String EmployerName;
    private String EmployerAddressLine1;
    private String EmployerAddressLine2;
    private String EmployerCity;
    private String EmployerState;
    private String EmployerCountry;
    private String Title;
    private String PlaceofBirth;
    private String Workphone;
    private String Homephone;
    private String SecondaryAddressLine1;
    private String SecondaryAddressLine2;
    private String SecondaryAddressCity;
    private String SecondaryAddressState;
    private String SecondaryAddressCountry;
    private String SpousesSurname;
    private String SpousesFirstname;
    private String SpousesMiddlename;

}
