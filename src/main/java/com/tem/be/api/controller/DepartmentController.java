package com.tem.be.api.controller;

import com.tem.be.api.dto.DepartmentDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.User;
import com.tem.be.api.service.DepartmentService;
import com.tem.be.api.utils.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing department related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * @param departmentService the service handling department operations
     */
    @Autowired
    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * Returns all departments.
     */
    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<Department>>> getAllDepartments() {
        log.info("DepartmentController.getAllDepartments() >> Entered");
        List<Department> departments = departmentService.getAllDepartments();
        ApiResponse<List<Department>> response = new ApiResponse<>(HttpStatus.OK.value(), "Departments fetched successfully", departments);
        log.info("DepartmentController.getAllDepartments() >> Exited");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Returns a department by its ID.
     *
     * @param id department ID
     */
    @GetMapping(value = "/getById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Department> getById(@PathVariable("id") Long id) {
        log.info("DepartmentController.getById() >> Entered with id: {}", id);
        Department department = departmentService.getDepartmentById(id).orElseThrow(() -> new ResourceNotFoundException("Department with id " + id + " not found"));
        log.info("DepartmentController.getById() >> Exited");
        return new ResponseEntity<>(department, HttpStatus.OK);
    }

    /**
     * Creates a new department.
     *
     * @param departmentDTO department details
     */
    @PostMapping(value = "/createDepartment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Department> createDepartment(@RequestBody DepartmentDTO departmentDTO) {
        log.info("DepartmentController.createDepartment() >> Entered");
        Department department = departmentService.createDepartment(departmentDTO);
        log.info("DepartmentController.createDepartment() >> Exited");
        return new ResponseEntity<>(department, HttpStatus.CREATED);
    }

    /**
     * Updates an existing department by ID.
     *
     * @param id department ID
     * @param departmentDTO updated department details
     */
    @PutMapping(value = "/updateById/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Department> updateDepartment(@PathVariable("id") Long id, @RequestBody DepartmentDTO departmentDTO) {
        log.info("DepartmentController.updateDepartment() >> Entered with id: {}", id);
        Department updatedDepartment = departmentService.updateDepartment(id, departmentDTO);
        log.info("DepartmentController.updateDepartment() >> Exited");
        return new ResponseEntity<>(updatedDepartment, HttpStatus.OK);
    }

    /**
     * Returns all users for a given department ID.
     *
     * @param id department ID
     */
    @GetMapping(value = "/getUsersOfADepartmentById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getUsersOfADepartmentById(@PathVariable("id") Long id) {
        log.info("DepartmentController.getUsersOfADepartmentById() >> Entered with id: {}", id);
        List<User> users = departmentService.getUsersOfADepartmentById(id);
        log.info("DepartmentController.getUsersOfADepartmentById() >> Exited");
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Deletes a department by ID.
     *
     * @param id department ID
     */
    @DeleteMapping("/deleteDepartmentById/{id}")
    public ResponseEntity<String> deleteDepartmentById(@PathVariable("id") Long id) {
        log.info("DepartmentController.deleteDepartmentById() >> Entered");
        departmentService.deleteDepartmentById(id);
        log.info("DepartmentController.deleteDepartmentById() >> Exited");
        return new ResponseEntity<>("Department deleted successfully!", HttpStatus.OK);
    }

}
