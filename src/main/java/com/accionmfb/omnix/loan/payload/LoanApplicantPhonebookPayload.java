/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.accionmfb.omnix.loan.payload;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
public class LoanApplicantPhonebookPayload {
    
      
    @NotNull(message = "contact name cannot be null")
    @NotEmpty(message = "contact name cannot be empty")
    @NotBlank(message = "contact name cannot be blank")
    private String contactName;
    @NotNull(message = "contact Mobile cannot be null")
    @NotEmpty(message = "contact Mobile be empty")
    @NotBlank(message = "contact Mobile be blank")
    private String contactMobile;
}
