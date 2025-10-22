package com.tem.be.api.service;

import com.tem.be.api.dao.RoleDao;
import com.tem.be.api.dto.RoleDTO;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.model.Role;
import com.tem.be.api.model.RoleModuleAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RoleServiceImpl class.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    RoleDao roleDao;

    @InjectMocks
    RoleServiceImpl roleServiceImpl;

    private static RoleDTO role= null;
    private static Role roleR= null;
    private static List<RoleModuleAccess> roleAccess = null;

    /**
     * Initializes reusable test data before all tests.
     */
    @BeforeAll
    static void init(){

        RoleModuleAccess access1 = new RoleModuleAccess();
        access1.setId(1L);
        access1.setModuleName("Role");
        access1.setAccessType("WRITE");

        RoleModuleAccess access2 = new RoleModuleAccess();
        access2.setId(2L);
        access2.setModuleName("User");
        access2.setAccessType("WRITE");

        roleAccess = List.of(access1, access2);

        role = new RoleDTO();
        role.setRoleName("UnitTestRole");
        role.setDescription("Role unit test description");
        role.setModuleAccessList(roleAccess);

        roleR = new Role();
        roleR.setRoleId(100L);
        roleR.setRoleName("UnitTestRole");
        roleR.setDescription("Role unit test description");
        roleR.setModuleAccessList(roleAccess);
    }

    /**
     * Tests successful role creation.
     */
    @Test
    void createRoleShouldAddProductSuccessfully() {
        when(roleDao.save(any(Role.class))).thenReturn(roleR);
        Role addedRole = roleServiceImpl.createRole(role);
        assertNotNull(addedRole);
        assertEquals(100L, addedRole.getRoleId());
        assertEquals(role.getRoleName(), addedRole.getRoleName());
    }

    /**
     * Tests that an exception is thrown for an invalid role name.
     */
    @Test
    void createRoleShouldThrowExceptionForInvalidRoleName(){
        role.setRoleName("");
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            roleServiceImpl.createRole(role);
        });
        assertEquals("Invalid Name Of Role",runtimeException.getMessage());
        verify(roleDao, never() ).save(any(Role.class));
    }

    /**
     * Tests soft deletion of a role by ID.
     */
    @Test
    void deleteRoleByIdShouldSoftDeleteRoleSuccessfully() {
        Long roleId = 100L;
        Role existingRole = new Role();
        existingRole.setRoleId(roleId);
        existingRole.setRoleName("ExistingRole");
        existingRole.setIsDeleted(false);
        when(roleDao.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleDao.save(any(Role.class))).thenReturn(existingRole);
        roleServiceImpl.deleteRoleById(roleId);
        assertTrue(existingRole.getIsDeleted());
        verify(roleDao).save(existingRole);
    }

    /**
     * Tests that an exception is thrown when deleting a non-existing role.
     */
    @Test
    void deleteRoleByIdShouldThrowExceptionWhenRoleNotFound() {
        Long invalidRoleId = 999L;
        when(roleDao.findById(invalidRoleId)).thenReturn(Optional.empty());
        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class, () -> {
            roleServiceImpl.deleteRoleById(invalidRoleId);
        });
        assertEquals("Role not found with ID: 999", exception.getMessage());
        verify(roleDao, never()).save(any(Role.class));
    }

    /**
     * Tests successful update of a role by ID.
     */
    @Test
    void updateRoleByIdShouldUpdateRoleSuccessfully() {
        Long roleId = 100L;

        Role existingRole = new Role();
        existingRole.setRoleId(roleId);
        existingRole.setRoleName("OldRole");
        existingRole.setDescription("Old Description");
        existingRole.setModuleAccessList(new ArrayList<>());

        // Mock the role fetch by ID
        when(roleDao.findById(roleId)).thenReturn(Optional.of(existingRole));
        // No conflict with new name
        when(roleDao.findByRoleNameAndIsDeletedFalse("NewRole")).thenReturn(Optional.empty());
        // Mock save
        when(roleDao.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleDTO updatedRoleDto = new RoleDTO();
        updatedRoleDto.setRoleName("NewRole");
        updatedRoleDto.setDescription("New Description");

        RoleModuleAccess access = new RoleModuleAccess();
        access.setModuleName("User");
        access.setAccessType("READ");

        updatedRoleDto.setModuleAccessList(List.of(access));

        Role result = roleServiceImpl.updateRoleById(roleId, updatedRoleDto);

        assertEquals("NewRole", result.getRoleName());
        assertEquals("New Description", result.getDescription());
        assertEquals(1, result.getModuleAccessList().size());
        verify(roleDao).save(any(Role.class));
    }

    /**
     * Tests that update fails if the new role name already exists.
     */
    @Test
    void updateRoleByIdShouldFailIfRoleNameAlreadyExists() {
        when(roleDao.findById(100L)).thenReturn(Optional.of(roleR));
        when(roleDao.findByRoleNameAndIsDeletedFalse("NewRole")).thenReturn(Optional.of(new Role()));

        RoleDTO update = new RoleDTO();
        update.setRoleName("NewRole");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            roleServiceImpl.updateRoleById(100L, update);
        });

        assertEquals("Role name already in use: "+ update.getRoleName(), ex.getMessage());
    }
}