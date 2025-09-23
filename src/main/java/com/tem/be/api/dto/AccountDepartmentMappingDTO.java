package com.tem.be.api.dto;

import lombok.Data;

/**
 * DTO for creating and updating Account-Department mappings.
 */
@Data
public class AccountDepartmentMappingDTO {
    private String foundationAccountNumber;
    private String departmentAccountNumber;
    private String department;
    private String carrier;
    private String createdBy;
}
