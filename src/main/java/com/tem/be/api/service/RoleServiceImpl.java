package com.tem.be.api.service;

import com.tem.be.api.dao.RoleDao;
import com.tem.be.api.dto.RoleDTO;
import com.tem.be.api.exception.InvalidRoleDataException;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.model.Role;
import com.tem.be.api.model.RoleModuleAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Service for role related operations.
 */
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleDao roleDao;
    private static final String ROLE_NOT_FOUND_MSG = "Role not found with ID: ";
    private static final String INVALID_ROLE = "Invalid Name Of Role";

    @Autowired
    public RoleServiceImpl(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    @Override
    public Role createRole(RoleDTO roleDTO) {

        if (roleDTO.getRoleName() == null || roleDTO.getRoleName().trim().isEmpty()) {
            throw new InvalidRoleDataException(INVALID_ROLE);
        }

        Optional<Role> existingRole = roleDao.findByRoleNameAndIsDeletedFalse(roleDTO.getRoleName());
        if (existingRole.isPresent()) {
            throw new ResourceAlreadyExistsException("Role already exists with the name: " + roleDTO.getRoleName());
        }

        Role role = new Role(roleDTO.getRoleName(), roleDTO.getDescription());

        // Create collection of RoleModuleAccess and link to Role
        List<RoleModuleAccess> accessList = roleDTO.getModuleAccessList().stream()
                .map(dto -> new RoleModuleAccess(dto.getModuleName(), dto.getAccessType(), role))
                .toList();

        // Set collection into role
        role.setModuleAccessList(accessList);

        // Persist everything at once
        return roleDao.save(role);

    }

    @Override
    public Role updateRoleById(Long id, RoleDTO roleDTO) {
        Role existingRole = roleDao.findById(id)
                .orElseThrow(() -> new ResourceAlreadyExistsException(ROLE_NOT_FOUND_MSG + id));

        if (!existingRole.getRoleName().equals(roleDTO.getRoleName())) {
            Optional<Role> roleByName = roleDao.findByRoleNameAndIsDeletedFalse(roleDTO.getRoleName());
            if (roleByName.isPresent()) {
                throw new ResourceAlreadyExistsException("Role name already in use: " + roleDTO.getRoleName());
            }
            existingRole.setRoleName(roleDTO.getRoleName());
        }

        existingRole.setDescription(roleDTO.getDescription());

        existingRole.getModuleAccessList().clear();
        existingRole.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        List<RoleModuleAccess> newAccessList = roleDTO.getModuleAccessList().stream()
                .map(dto -> new RoleModuleAccess(dto.getModuleName(), dto.getAccessType(), existingRole))
                .toList();

        existingRole.getModuleAccessList().addAll(newAccessList);

        return roleDao.save(existingRole);
    }

    @Override
    public void deleteRoleById(Long id) {
        Role existingRole = roleDao.findById(id)
                .orElseThrow(() -> new ResourceAlreadyExistsException(ROLE_NOT_FOUND_MSG + id));

        existingRole.setIsDeleted(true); // or setIsActive(false)
        roleDao.save(existingRole); // update the record
    }

    @Override
    public List<Role> getAllRoles() {
        return roleDao.findByIsDeletedFalseOrderByUpdatedAtDesc();
    }
}
