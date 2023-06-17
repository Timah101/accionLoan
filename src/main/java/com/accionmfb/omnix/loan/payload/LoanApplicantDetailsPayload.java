/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

/**
 *
 * @author dakinkuolie
 */
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
public class LoanApplicantDetailsPayload {

    @NotNull(message = "Phonebook cannot be null")
    @NotEmpty(message = "Phonebook cannot be empty")
    @NotBlank(message = "Phonebook cannot be blank")
    private String phonebook;
    @NotNull(message = "Geolocation cannot be null")
    @NotEmpty(message = "Geolocation cannot be empty")
    @NotBlank(message = "Geolocation cannot be blank")
    private String geolocation;
    @NotNull(message = "picture cannot be null")
    @NotEmpty(message = "picture cannot be empty")
    @NotBlank(message = "picture cannot be blank")
    private String picture;
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
