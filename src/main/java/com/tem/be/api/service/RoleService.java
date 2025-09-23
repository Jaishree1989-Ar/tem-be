package com.tem.be.api.service;

import com.tem.be.api.dto.RoleDTO;
import com.tem.be.api.model.Role;

import java.util.List;

public interface RoleService {
    Role createRole(RoleDTO roleDTO);

    Role updateRoleById(Long id, RoleDTO roleDTO);

    void deleteRoleById(Long id);

    List<Role> getAllRoles();
}
