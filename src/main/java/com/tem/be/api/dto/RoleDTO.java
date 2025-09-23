package com.tem.be.api.dto;

import com.tem.be.api.model.RoleModuleAccess;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing role information.
 * Used to transfer role related data between layers of the application.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO {
    private String roleName;
    private String description;
    private List<RoleModuleAccess> moduleAccessList;
}
