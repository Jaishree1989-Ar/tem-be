package com.tem.be.api.service;

import com.tem.be.api.dao.CityDao;
import com.tem.be.api.dao.DepartmentDao;
import com.tem.be.api.dto.DepartmentDTO;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Service for department related operations.
 */
@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentDao departmentDao;
    private final CityDao cityDao;

    private static final String DEPT_NOT_FOUND_MSG = "Department not found with id ";
    private static final String CITY_NOT_FOUND_MSG = "City not found with id ";

    @Autowired
    public DepartmentServiceImpl(DepartmentDao departmentDao, CityDao cityDao) {
        this.departmentDao = departmentDao;
        this.cityDao = cityDao;
    }

    @Override
    public List<Department> getAllDepartments() {
        return departmentDao.findByIsDeletedFalse();
    }

    @Override
    public Optional<Department> getDepartmentById(Long id) {
        return departmentDao.findById(id);
    }

    @Override
    public Department createDepartment(DepartmentDTO departmentDTO) {
        Optional<Department> existingDept = departmentDao.findByDeptNameAndIsDeletedFalse(departmentDTO.getDeptName());
        if (existingDept.isPresent()) {
            throw new ResourceAlreadyExistsException("Department already exists with name: " + departmentDTO.getDeptName());
        }
        return departmentDao.save(new Department(departmentDTO.getDeptName(), departmentDTO.getDeptNumber(), departmentDTO.getDescription(), Timestamp.valueOf(departmentDTO.getLastInvoicedAt()), cityDao.findById(departmentDTO.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException(CITY_NOT_FOUND_MSG + departmentDTO.getCityId()))));
    }


    @Override
    public Department updateDepartment(Long id, DepartmentDTO departmentDTO) {
        Department dept = departmentDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DEPT_NOT_FOUND_MSG + id));

        if (!dept.getDeptName().equals(departmentDTO.getDeptName())) {
            Optional<Department> existingDept = departmentDao.findByDeptNameAndIsDeletedFalse(departmentDTO.getDeptName());
            if (existingDept.isPresent()) {
                throw new ResourceAlreadyExistsException("Department name already in use: " + departmentDTO.getDeptName());
            }
        }
        dept.setDeptName(departmentDTO.getDeptName());
        dept.setDeptNumber(departmentDTO.getDeptNumber());
        dept.setDescription(departmentDTO.getDescription());
        dept.setLastInvoicedAt(Timestamp.valueOf(departmentDTO.getLastInvoicedAt()));
        dept.setCity(cityDao.findById(departmentDTO.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException(CITY_NOT_FOUND_MSG + departmentDTO.getCityId())));
        return departmentDao.save(dept);
    }

    @Override
    public List<User> getUsersOfADepartmentById(Long id) {
        Department department = departmentDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DEPT_NOT_FOUND_MSG + id));
        return department.getUsers();
    }

    @Override
    public void deleteDepartmentById(Long id) {
        Department existingDepartment = departmentDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DEPT_NOT_FOUND_MSG + id));
        existingDepartment.setIsDeleted(true);
        departmentDao.save(existingDepartment);
    }

}
