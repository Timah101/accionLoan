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
public class CreditBureauRequestPayload {

    @NotNull(message = "BVN cannot be null")
    @NotEmpty(message = "BVN cannot be empty")
    @NotBlank(message = "BVN cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit BVN required")
    private String bvn;
    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "Search type cannot be null")
    @NotEmpty(message = "Search type cannot be empty")
    @NotBlank(message = "Search type cannot be blank")
    @Pattern(regexp = "^(ALL|ADDRESS|CLASSIFICATION|CONTACT|CREDITHISTORY|CREDITRISK|IDENTIFICATION|INQUIRYPRODUCT|INQUIRYHISTORY|PERFORMANCEHISTORY)$", message = "Value must be either ALL, ADDRESS, CLASSIFICATION, CONTACT, CREDIT HISTORY, CREDIT RISK, IDENTIFICATION, INQUIRY PRODUCT, INQUIRY HISTORY or PERFORMANCE HISTORY")
    private String searchType;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotNull(message = "Token cannot be null")
    @NotEmpty(message = "Token cannot be empty")
    @NotBlank(message = "Token cannot be blank")
    private String token;
}
