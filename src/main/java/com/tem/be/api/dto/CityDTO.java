package com.tem.be.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) representing city information.
 * Used to transfer city related data between layers of the application.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CityDTO {
    private String cityName;
}
