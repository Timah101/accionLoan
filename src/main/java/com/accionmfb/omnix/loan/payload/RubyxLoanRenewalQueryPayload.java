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
public class RubyxLoanRenewalQueryPayload {

    @NotNull(message = "Start date cannot be null")
    @NotEmpty(message = "Start date cannot be empty")
    @NotBlank(message = "Start date cannot be blank")
    @Pattern(regexp = "^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$", message = "Start date must be like 2021-01-13")
    private String startDate;
    @NotNull(message = "End date cannot be null")
    @NotEmpty(message = "End date cannot be empty")
    @NotBlank(message = "End date cannot be blank")
    @Pattern(regexp = "^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$", message = "End date must be like 2021-01-13")
    private String endDate;
//    @NotNull(message = "Customer number cannot be null")
//    @NotEmpty(message = "Customer number cannot be empty")
//    @NotBlank(message = "Customer number cannot be blank")
    @Pattern(regexp = "[0-9]{1,}", message = "Valid customer number (CIF) required")
    private String customerNumber;
//    @NotNull(message = "Product code cannot be null")
//    @NotEmpty(message = "Product code cannot be empty")
//    @NotBlank(message = "Product code cannot be blank")
    @Pattern(regexp = "[0-9]{4,5}", message = "4 or 5 digit product code required")
    private String productCode;
    @Pattern(regexp = "^(True|true|False|false)$", message = "Update value must be True or False required")
    private String customerApply;
    @Pattern(regexp = "^(True|true|False|false)$", message = "Update value must be True or False required")
    private String customerSignOfferLetter;
    @Pattern(regexp = "^(True|true|False|false)$", message = "Update value must be True or False required")
    private String guarantorSignOfferLetter;
    @Pattern(regexp = "^(True|true|False|false)$", message = "Update value must be True or False required")
    private String creditBureauSearchDone;
    @Pattern(regexp = "^(True|true|False|false)$", message = "Update value must be True or False required")
    private String guarantorIdVerified;
    @Pattern(regexp = "[0-9]{1,}", message = "2 digit loan cycle required")
    private String loanCycle;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}
