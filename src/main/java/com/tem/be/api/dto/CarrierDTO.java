package com.tem.be.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) representing carrier information.
 * Used to transfer carrier related data between layers of the application.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CarrierDTO {
    private String carrierName;
    private String carrierNumber;
    private String description;
    private String info;
}
