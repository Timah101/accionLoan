 
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
public class CardDetailsPayload {
    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]" )
    private String mobileNumber;
    @NotNull(message = "Card number cannot be null")
    @NotEmpty(message = "Card number cannot be empty")
    @NotBlank(message = "Card number cannot be blank")     
    private String cardNumber;
    @NotNull(message = "validity Date cannot be null")
    @NotEmpty(message = "validity Date cannot be empty")
    @NotBlank(message = "validity Date cannot be blank")    
    private String validityDate;
    @NotNull(message = "CVV cannot be null")
    @NotEmpty(message = "CVV cannot be empty")
    @NotBlank(message = "CVV cannot be blank")
    @Pattern(regexp = "[0-9]{3}", message = "CVV is required")
    private String cvv;
    
   @NotNull(message = "PIN cannot be null")
    @NotEmpty(message = "PIN cannot be empty")
    @NotBlank(message = "PIN cannot be blank")
    @Pattern(regexp = "[0-9]{4}", message = "4 digit other officer required")
    private String pin;
    /* @NotNull(message = "Product code cannot be null")
    @NotEmpty(message = "Product code cannot be empty")
    @NotBlank(message = "Product code cannot be blank")
    @Pattern(regexp = "[0-9]{4,5}", message = "4 or 5 digit product code required")
    private String productCode; */
    
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}
