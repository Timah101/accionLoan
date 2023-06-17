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
 * @author dakinkuolie
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GuarantorUpdatePayload {

    @NotNull(message = "First Guarantor name cannot be null")
    @NotEmpty(message = "First Guarantor name cannot be empty")
    @NotBlank(message = "First Guarantor name cannot be blank")

    private String guarantorname1;

    @NotNull(message = "Second Guarantor name cannot be null")
    @NotEmpty(message = "Second Guarantor name cannot be empty")
    @NotBlank(message = "Second Guarantor name cannot be blank")

    private String guarantorname2;

    @NotNull(message = "First Guarantor Mobile number cannot be null")
    @NotEmpty(message = "First Guarantor Mobile number cannot be empty")
    @NotBlank(message = "First Guarantor Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String guarantormobile1;

    @NotNull(message = "Second Guarantor Mobile number cannot be null")
    @NotEmpty(message = "Second Guarantor Mobile number cannot be empty")
    @NotBlank(message = "Second Guarantor Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String guarantormobile2;

    @NotNull(message = "Customer Mobile number cannot be null")
    @NotEmpty(message = "Customer Mobile number cannot be empty")
    @NotBlank(message = "Customer Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
