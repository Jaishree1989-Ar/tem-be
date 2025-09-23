package com.tem.be.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object (DTO) for total(sum of total column) by carrier and department.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentCarrierPlansDTO {
    private String department;
    private List<PlanCarrierDTO> carriers;
    private int totalPlans;
}
