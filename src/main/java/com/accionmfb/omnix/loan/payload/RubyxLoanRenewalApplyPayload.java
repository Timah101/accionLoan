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
public class RubyxLoanRenewalApplyPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "[0-9]{10}", message = "Account number must be 11 digit")
    @Schema(name = "Account Number", example = "0123456789", description = "1 or more digit account number")
    private String accountNumber;
    @NotBlank(message = "Loan amount is required")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Loan Amount must contain only digits, comma or dot only")
    @Schema(name = "Loan Amount", example = "1,000.00", description = "Loan Amount")
    private String loanAmount;
//    @NotBlank(message = "Loan Tenor is required")
//    @Pattern(regexp = "[1-]{1,2}", message = "Loan Tenor must be 1-2 digit")
//    @Schema(name = "Loan Tenor", example = "1,12", description = "Loan Tenor")
    private String loanTenor;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}
