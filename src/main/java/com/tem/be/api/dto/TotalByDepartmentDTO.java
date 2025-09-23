package com.tem.be.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for total(sum of total column) by department.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TotalByDepartmentDTO {
    private String department;
    private BigDecimal total;
}
