package com.tem.be.api.dto;

import java.math.BigDecimal;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * DTO for carrying the new value for recurring charges in a PATCH/PUT request.
 */
@Data
public class UpdateRecurringChargesRequest {

    @NotNull(message = "The new recurring charges value cannot be null.")
    private BigDecimal newRecurringCharges;

    @NotBlank(message = "Carrier must be specified.")
    private String carrier;

}
