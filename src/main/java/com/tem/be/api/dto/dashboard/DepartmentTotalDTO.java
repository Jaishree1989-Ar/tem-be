package com.tem.be.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object (DTO) for expense summary related by department.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentTotalDTO {
    private String department;
    private List<CarrierTotalDTO> carriers;
    private BigDecimal total;
}
