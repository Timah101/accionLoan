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
public class RubyxLoanRenewalUpdatePayload {

    @NotNull(message = "Update type cannot be null")
    @NotEmpty(message = "Update type cannot be empty")
    @NotBlank(message = "Update type cannot be blank")
    @Pattern(regexp = "^(Guarantor|Customer|Id Validation|Disbursement Account|Loan Cycle|Interest Rate|Credit Bureau|Brighta Commitment)$", message = "Update type must be Customer, Guarantor or Id Validation required")
    private String updateType;
    @NotNull(message = "Update value cannot be null")
    @NotEmpty(message = "Update value cannot be empty")
    @NotBlank(message = "Update value cannot be blank")
    @Pattern(regexp = "^(True|true|False|false|[0-9]{10}|[0-9.]{1,})$", message = "Update value must be True/False, 10 digit NUBAN, fractional interest rate, single digit loan cycle required")
    private String updateValue;
    @NotNull(message = "Loan ID number is required")
    @NotBlank(message = "Loan ID number is required")
    private String loanId;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
