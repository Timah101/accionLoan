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
public class LoanTypeRequestPayload {

    @NotNull(message = "Loan name cannot be null")
    @NotEmpty(message = "Loan name cannot be empty")
    @NotBlank(message = "Loan name cannot be blank")
    private String loanName;
    @NotNull(message = "Loan description cannot be null")
    @NotEmpty(message = "Loan description cannot be empty")
    @NotBlank(message = "Loan description cannot be blank")
    private String loanDescription;
    @NotNull(message = "Loan category cannot be null")
    @NotEmpty(message = "Loan category cannot be empty")
    @NotBlank(message = "Loan category cannot be blank")
    @Pattern(regexp = "^(ARTISAN|DIGITAL|BAU|Artisan|Digital)$", message = "Loan Category must be either Digital, Artisan or BAU")
    private String loanCategory;
    @NotNull(message = "Minimum tenor cannot be null")
    @NotEmpty(message = "Minimum tenor cannot be empty")
    @NotBlank(message = "Minimum tenor cannot be blank")
    @Pattern(regexp = "[0-9]{1,12}", message = "Minimum tenor must be bwtween 0-12")
    private String minTenor;
    @NotNull(message = "Maximum tenor cannot be null")
    @NotEmpty(message = "Maximum tenor cannot be empty")
    @NotBlank(message = "Maximum tenor cannot be blank")
    @Pattern(regexp = "[0-9]{1,12}", message = "Minimum tenor must be bwtween 0-12")
    private String maxTenor;
    @NotNull(message = "Minimum amount cannot be null")
    @NotEmpty(message = "Minimum amount cannot be empty")
    @NotBlank(message = "Minimum amount cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Minimum Loan Amount must contain only digits, comma or dot only")
    private String minAmount;
    @NotNull(message = "Maximum amount cannot be null")
    @NotEmpty(message = "Maximum amount cannot be empty")
    @NotBlank(message = "Maximum amount cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Maximum Loan Amount must contain only digits, comma or dot only")
    private String maxAmount;
    @NotNull(message = "Interest rate cannot be null")
    @NotEmpty(message = "Interest rate cannot be empty")
    @NotBlank(message = "Interest rate cannot be blank")
    @Pattern(regexp = "^\\d{1,2}(\\.\\d{1,2})?%$", message = "Interest rate must be digits like 3% or 3.2%")
    private String interestRate;
    @NotNull(message = "Admin fee cannot be null")
    @NotEmpty(message = "Admin fee cannot be empty")
    @NotBlank(message = "Admin cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Admin Fee must be like 1,000 or 1000")
    private String adminFee;
    @NotNull(message = "Insurance fee cannot be null")
    @NotEmpty(message = "Insurance fee cannot be empty")
    @NotBlank(message = "Insurance fee cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Insurance Fee must be like 1,000 or 1000")
    private String insuranceFee;
    @NotNull(message = "Processing fee cannot be null")
    @NotEmpty(message = "Processing fee cannot be empty")
    @NotBlank(message = "Processing fee cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Processing Fee must be like 1,000 or 1000")
    private String processingFee;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
