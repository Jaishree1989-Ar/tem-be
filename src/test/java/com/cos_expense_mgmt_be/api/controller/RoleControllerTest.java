package com.tem.be.api.controller;

import com.tem.be.api.dto.RoleDTO;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.model.Role;
import com.tem.be.api.service.RoleService;
import com.tem.be.api.utils.RestConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the RoleController using MockMvc.
 */
@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Tests successful role creation.
     */
    @Test
    void createRoleShouldReturnSuccessResponse() throws Exception {
        // Given
        RoleDTO dto = new RoleDTO();
        dto.setRoleName("Admin");
        dto.setDescription("Administrator Role");

        Role createdRole = new Role();
        createdRole.setRoleId(1L);
        createdRole.setRoleName("Admin");
        createdRole.setDescription("Administrator Role");

        when(roleService.createRole(any(RoleDTO.class))).thenReturn(createdRole);

        // When & Then
        mockMvc.perform(post("/role/createRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(RestConstants.SUCCESS_CODE))
                .andExpect(jsonPath("$.status").value(RestConstants.SUCCESS_STRING))
                .andExpect(jsonPath("$.data.roleName").value("Admin"));
    }

    /**
     * Tests error response when role name is missing during creation.
     */
    @Test
    void createRoleShouldReturnErrorWhenRoleNameIsMissing() throws Exception {
        RoleDTO dto = new RoleDTO(); // roleName is null
        dto.setDescription("Some role");

        when(roleService.createRole(any(RoleDTO.class)))
                .thenThrow(new RuntimeException("Invalid Name Of Role"));

        mockMvc.perform(post("/role/createRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Invalid Name Of Role"));
    }

    /**
     * Tests error response when role name already exists.
     */
    @Test
    void createRoleShouldReturnErrorWhenRoleAlreadyExists() throws Exception {
        RoleDTO dto = new RoleDTO();
        dto.setRoleName("Admin");
        dto.setDescription("Duplicate Role");

        when(roleService.createRole(any(RoleDTO.class)))
                .thenThrow(new ResourceAlreadyExistsException("Role already exists with the name: Admin"));

        mockMvc.perform(post("/role/createRole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Role already exists with the name: Admin"));
    }

    /**
     * Tests successful role update by ID.
     */
    @Test
    void updateRoleShouldReturnUpdatedRole() throws Exception {
        Long roleId = 1L;

        RoleDTO dto = new RoleDTO();
        dto.setRoleName("UpdatedRole");
        dto.setDescription("Updated description");

        Role updatedRole = new Role();
        updatedRole.setRoleId(roleId);
        updatedRole.setRoleName("UpdatedRole");
        updatedRole.setDescription("Updated description");

        when(roleService.updateRoleById(eq(roleId), any(RoleDTO.class))).thenReturn(updatedRole);

        mockMvc.perform(put("/role/updateRoleById/{id}", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(RestConstants.SUCCESS_CODE))
                .andExpect(jsonPath("$.status").value(RestConstants.SUCCESS_STRING))
                .andExpect(jsonPath("$.data.roleId").value(1L))
                .andExpect(jsonPath("$.data.roleName").value("UpdatedRole"));
    }

    /**
     * Tests error response when updating a non-existing role.
     */
    @Test
    void updateRoleShouldReturnErrorWhenRoleNotFound() throws Exception {
        Long invalidId = 999L;

        RoleDTO dto = new RoleDTO();
        dto.setRoleName("TestRole");
        dto.setDescription("Test");

        when(roleService.updateRoleById(eq(invalidId), any(RoleDTO.class)))
                .thenThrow(new RuntimeException("Role not found"));

        mockMvc.perform(put("/role/updateRoleById/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Role not found"));
    }

    /**
     * Tests successful soft deletion of a role by ID.
     */
    @Test
    void deleteRoleShouldReturnSuccessMessage() throws Exception {
        Long roleId = 1L;
        mockMvc.perform(delete("/role/deleteRoleById/{id}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role deleted successfully!"));
    }

    /**
     * Tests successful retrieval of all roles.
     */
    @Test
    void getAllRolesShouldReturnRoleListSuccessfully() throws Exception {
        List<Role> roles = List.of(
                new Role("Admin", "Administrator role"),
                new Role("User", "Standard user role")
        );

        when(roleService.getAllRoles()).thenReturn(roles);

        mockMvc.perform(get("/role/getAllRoles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(RestConstants.SUCCESS_CODE))
                .andExpect(jsonPath("$.status").value(RestConstants.SUCCESS_STRING))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].roleName").value("Admin"))
                .andExpect(jsonPath("$.data[1].roleName").value("User"));
    }

    /**
     * Tests error response when an exception occurs while retrieving all roles.
     */
    @Test
    void getAllRolesShouldReturnErrorWhenExceptionThrown() throws Exception {
        when(roleService.getAllRoles()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/role/getAllRoles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Database error"));
    }
}