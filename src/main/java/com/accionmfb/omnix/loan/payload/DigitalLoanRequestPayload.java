/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
public class DigitalLoanRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "BVN cannot be null")
    @NotEmpty(message = "BVN cannot be empty")
    @NotBlank(message = "BVN cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit BVN required")
    private String bvn;

    @NotNull(message = "Loan type cannot be null")
    @NotEmpty(message = "Loan type cannot be empty")
    @NotBlank(message = "Loan type cannot be blank")
    @Pattern(regexp = "[A-Za-z0-9]{1,}", message = "Loan type is required")
    private String loanType;
    @NotBlank(message = "Loan amount is required")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Loan Amount must contain only digits, comma or dot only")
    @Schema(name = "Loan Amount", example = "1,000.00", description = "Loan Amount")
    private String loanAmount;
    @NotBlank(message = "Loan Tenor is required")
    @Pattern(regexp = "[1-6]{1}", message = "Loan Tenor must be 1 digit")
    @Schema(name = "Loan Tenor", example = "1", description = "Loan Tenor")
    private String loanTenor;
    @NotNull(message = "Loan Purpose cannot be null")
    @NotEmpty(message = "Loan Purpose cannot be empty")
    @NotBlank(message = "Loan Purpose cannot be blank")
    private String loanPurpose;
    @NotBlank(message = "Disbursement account is required")
    @Pattern(regexp = "[0-9]{10}", message = "Disbursement account must be 10 digit disbursement account number")
    @Schema(name = "Disbursement Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String disbursementAccount;
    @NotNull(message = "Business Type is required")
    @NotBlank(message = "Business Type business is required")
    @Schema(name = "Business Type", example = "Farming", description = "Farming")
    private String businessType;
    @NotNull(message = "Business name cannot be null")
    @NotEmpty(message = "Business name cannot be empty")
    @NotBlank(message = "Business name cannot be blank")
    private String businessName;
    @NotNull(message = "Business address cannot be null")
    @NotEmpty(message = "Business address cannot be empty")
    @NotBlank(message = "Business address cannot be blank")
    private String businessAddress;
    @NotNull(message = "Behavioural analysis is required")
    private String monoAccountCode;

    @NotNull(message = "Marital Status cannot be null")
    @NotEmpty(message = "Marital Status cannot be empty")
    @NotBlank(message = "Marital Status cannot be blank")
    private String maritalStatus;
    @NotNull(message = "Spouse name cannot be null")
    @NotEmpty(message = "Spouse name cannot be empty")
    @NotBlank(message = "Spouse name cannot be blank")
    private String nameOfSpouse;

    @NotNull(message = "Number of Dependent cannot be null")
    @NotEmpty(message = "Number of Dependent cannot be empty")
    @NotBlank(message = "Number of Dependent cannot be blank")
    @Pattern(regexp = "[0-9]")
    private String noOfDependent;

    @NotNull(message = "Type of Residence cannot be null")
    @NotEmpty(message = "Type of Residence cannot be empty")
    @NotBlank(message = "Type of Residence cannot be blank")
    private String typeOfResidence;
    @NotNull(message = "Rent per year cannot be null")
    @NotEmpty(message = "Rent per year cannot be empty")
    @NotBlank(message = "Rent per year cannot be blank")
    private String rentPerYear;
    @NotNull(message = "Year of Residency cannot be null")
    @NotEmpty(message = "Year of Residency cannot be empty")
    @NotBlank(message = "Year of Residency cannot be blank")
    private String yearOfResidency;
    @NotNull(message = "Customer Business is SaveBrighta")
    @NotEmpty(message = "Customer Business is SaveBrighta")
    @NotBlank(message = "Customer Business is SaveBrighta")
    private String customerBusiness;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
//    @NotBlank(message = "Hash value is required")
//    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
//    private String hash;
    private String imei;
    private String paystackRef;
}
