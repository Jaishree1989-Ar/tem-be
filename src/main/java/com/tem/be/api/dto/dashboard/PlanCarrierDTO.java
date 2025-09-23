package com.tem.be.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object (DTO) for service plans by carrier.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlanCarrierDTO {
    private String carrier;
    private List<String> plans;
}
