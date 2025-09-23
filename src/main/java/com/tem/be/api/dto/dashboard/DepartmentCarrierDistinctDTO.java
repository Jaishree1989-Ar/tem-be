package com.tem.be.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object (DTO) for distinct departments and carriers.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentCarrierDistinctDTO {
        private List<String> departments;
        private List<String> carriers;
        private List<String> deviceClasses;

}
