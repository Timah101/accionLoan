/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
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
public class EmailRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "Recipient name cannot be null")
    @NotEmpty(message = "Recipient name cannot be empty")
    @NotBlank(message = "Recipient name cannot be blank")
    private String recipientName;
    @NotNull(message = "Recipient email cannot be null")
    @NotEmpty(message = "Recipient email cannot be empty")
    @NotBlank(message = "Recipient email cannot be blank")
    @Email
    private String recipientEmail;
    @NotNull(message = "Email subject cannot be null")
    @NotEmpty(message = "Email subject cannot be empty")
    @NotBlank(message = "Email subject cannot be blank")
    private String subject;
    @NotNull(message = "Email body cannot be null")
    @NotEmpty(message = "Email body cannot be empty")
    @NotBlank(message = "Email body cannot be blank")
    private String emailBody;
    private String attachmentFilePath;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotNull(message = "Email login username cannot be null")
    @NotEmpty(message = "Email login username cannot be empty")
    @NotBlank(message = "Email login username cannot be blank")
    private String emailLoginUsername;
    @NotNull(message = "Email login password cannot be null")
    @NotEmpty(message = "Email login password cannot be empty")
    @NotBlank(message = "Email login password cannot be blank")
    private String emailLoginPassword;
    @NotNull(message = "Token cannot be null")
    @NotEmpty(message = "Token cannot be empty")
    @NotBlank(message = "Token cannot be blank")
    private String token;
}
