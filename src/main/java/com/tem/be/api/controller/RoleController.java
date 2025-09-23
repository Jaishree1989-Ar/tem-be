package com.tem.be.api.controller;

import com.tem.be.api.dto.RoleDTO;
import com.tem.be.api.model.Role;
import com.tem.be.api.service.RoleService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing role related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/role")
public class RoleController {
    private final RoleService roleService;

    /**
     * @param roleService the service handling role operations
     */
    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Creates a new role.
     *
     * @param roleDTO role details
     */
    @PostMapping(value = "/createRole", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody RoleDTO roleDTO) {
        log.info("RoleController.createRole() >> Entered");
        Role roleDetails = roleService.createRole(roleDTO);
        ApiResponse<Role> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, roleDetails);
        log.info("RoleController.createRole() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing role by ID.
     *
     * @param id role ID
     * @param roleDTO updated role details
     */
    @PutMapping(value = "/updateRoleById/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Role>> updateRoleById(@PathVariable("id") Long id, @RequestBody RoleDTO roleDTO) {
        log.info("RoleController.updateRoleById() >> Entered");
        Role roleDetails = roleService.updateRoleById(id, roleDTO);
        ApiResponse<Role> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, roleDetails);
        log.info("RoleController.updateRoleById() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a role by ID.
     *
     * @param id role ID
     */
    @DeleteMapping("/deleteRoleById/{id}")
    public ResponseEntity<Map<String, String>> deleteRoleById(@PathVariable("id") Long id) {
        log.info("RoleController.deleteRoleById() >> Entered");
        roleService.deleteRoleById(id);
        log.info("RoleController.deleteRoleById() >> Exited");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Role deleted successfully!");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Returns all roles.
     */
    @GetMapping(value = "/getAllRoles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        log.info("RoleController.getAllRoles() >> Entered");
        List<Role> roles = roleService.getAllRoles();
        ApiResponse<List<Role>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, roles);
        log.info("RoleController.getAllRoles() >> Exited");
        return ResponseEntity.ok(response);
    }

}
