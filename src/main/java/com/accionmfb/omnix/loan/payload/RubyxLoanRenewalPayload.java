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
public class RubyxLoanRenewalPayload {

    @NotNull(message = "Customer number cannot be null")
    @NotEmpty(message = "Customer number cannot be empty")
    @NotBlank(message = "Customer number cannot be blank")
    @Pattern(regexp = "[0-9]{1,}", message = "Valid customer number (CIF) required")
    private String customerNumber;
    @NotNull(message = "Account officer cannot be null")
    @NotEmpty(message = "Account officer cannot be empty")
    @NotBlank(message = "Account officer cannot be blank")
    @Pattern(regexp = "[0-9]{4}", message = "4 digit account officer required")
    private String accountOfficer;
    @NotNull(message = "Branch code cannot be null")
    @NotEmpty(message = "Branch code cannot be empty")
    @NotBlank(message = "Branch code cannot be blank")
    @Pattern(regexp = "^[A-Za-z]{2}[0-9]{7}$", message = "Branch code like NG0010000 required")
    private String branchCode;
    @NotNull(message = "Renewal score cannot be null")
    @NotEmpty(message = "Renewal score cannot be empty")
    @NotBlank(message = "Renewal score cannot be blank")
    private String renewalScore;
    @NotNull(message = "Renewal rating cannot be null")
    @NotEmpty(message = "Renewal rating cannot be empty")
    @NotBlank(message = "Renewal rating cannot be blank")
    @Pattern(regexp = "^[A-Ea-e]{1}$", message = "Rating like A, B, C, D or E is required")
    private String renewalRating;
    @NotBlank(message = "Renewal amount is required")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Renewal amount must contain only digits, comma or dot only")
    private String renewalAmount;
    @NotNull(message = "Product code cannot be null")
    @NotEmpty(message = "Product code cannot be empty")
    @NotBlank(message = "Product code cannot be blank")
    private String productCode;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
     private String eligibilityEndDate;
}
