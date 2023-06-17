/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.payload;

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
 * @author ofasina
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContactRequestPayload {
    
    @NotNull(message = "Phone number cannot be null")
    @NotEmpty(message = "Phone number cannot be empty")
    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String phoneNumber;
    @NotNull(message = "Geolocation cannot be null")
    @NotEmpty(message = "Geolocation cannot be empty")
    @NotBlank(message = "Geolocation cannot be blank")
    private String name;
    
}
