package com.tem.be.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) total(sum of total column) by carriers.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CarrierTotalDTO {
    private String carrier;
    private String account;
    private BigDecimal total;
}
