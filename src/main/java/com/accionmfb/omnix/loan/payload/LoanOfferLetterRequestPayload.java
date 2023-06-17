/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class LoanOfferLetterRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "Search type cannot be null")
    @NotEmpty(message = "Search type cannot be empty")
    @NotBlank(message = "Search type cannot be blank")
    @Pattern(regexp = "^(CEF|LoanID|LoanId)$", message = "Search type must be either CEF or LoanID")
    private String searchType;
    @NotNull(message = "Search ID cannot be null")
    @NotEmpty(message = "Search ID cannot be empty")
    @NotBlank(message = "Search ID cannot be blank")
    private String searchId;
    @NotNull(message = "Loan offer type cannot be null")
    @NotEmpty(message = "Loan offer type cannot be empty")
    @NotBlank(message = "Loan offer type cannot be blank")
    @Pattern(regexp = "^(Individual|Promissory|Guarantee|PayGo|PayGoAccrual|PayGoPromissory|BillOfSales|School|NA)$", message = "Loan offer type must be either Individual, Promissory, Guarantee, PayGo, PayGoAccrual, PayGoPromissory, BillOfSales, School or NA")
    private String offerType;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
