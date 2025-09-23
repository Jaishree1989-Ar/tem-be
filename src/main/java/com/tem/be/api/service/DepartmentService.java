package com.tem.be.api.service;

import com.tem.be.api.dto.DepartmentDTO;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.User;

import java.util.List;
import java.util.Optional;

public interface DepartmentService {

    List<Department> getAllDepartments();

    Optional<Department> getDepartmentById(Long id);

    Department createDepartment(DepartmentDTO departmentDTO);

    Department updateDepartment(Long id, DepartmentDTO departmentDTO);

    List<User> getUsersOfADepartmentById(Long id);

    void deleteDepartmentById(Long id);
}
