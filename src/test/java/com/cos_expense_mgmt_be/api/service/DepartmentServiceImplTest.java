package com.tem.be.api.service;

import com.tem.be.api.dao.CityDao;
import com.tem.be.api.dao.DepartmentDao;
import com.tem.be.api.dto.DepartmentDTO;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.City;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentDao departmentDao;

    @Mock
    private CityDao cityDao;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department department;
    private DepartmentDTO dto;
    private City city;

    @BeforeEach
    void setUp() {
        city = new City();
        city.setCityId(1L);

        department = new Department();
        department.setDeptId(1L);
        department.setDeptName("IT");
        department.setDeptNumber("001");
        department.setDescription("Technology");
        department.setCity(city);
        department.setLastInvoicedAt(new Timestamp(System.currentTimeMillis()));
        department.setIsDeleted(false);

        dto = new DepartmentDTO();
        dto.setDeptName("IT");
        dto.setDeptNumber("001");
        dto.setDescription("Technology");
        dto.setCityId(1L);
        dto.setLastInvoicedAt("2025-07-28 10:00:00");
    }

    @Test
    void getAllDepartmentsShouldReturnList() {
        when(departmentDao.findByIsDeletedFalse()).thenReturn(List.of(department));
        List<Department> result = departmentService.getAllDepartments();
        assertEquals(1, result.size());
        verify(departmentDao).findByIsDeletedFalse();
    }

    @Test
    void getDepartmentByIdShouldReturnDepartmentWhenExists() {
        when(departmentDao.findById(1L)).thenReturn(Optional.of(department));
        Optional<Department> result = departmentService.getDepartmentById(1L);
        assertTrue(result.isPresent());
        assertEquals("IT", result.get().getDeptName());
    }

    @Test
    void getDepartmentByIdShouldReturnEmptyWhenNotExists() {
        when(departmentDao.findById(2L)).thenReturn(Optional.empty());
        Optional<Department> result = departmentService.getDepartmentById(2L);
        assertTrue(result.isEmpty());
    }

    @Test
    void createDepartmentShouldCreateWhenValid() {
        when(departmentDao.findByDeptNameAndIsDeletedFalse("IT")).thenReturn(Optional.empty());
        when(cityDao.findById(1L)).thenReturn(Optional.of(city));
        when(departmentDao.save(any(Department.class))).thenReturn(department);

        Department result = departmentService.createDepartment(dto);
        assertEquals("IT", result.getDeptName());
    }

    @Test
    void createDepartmentShouldThrowWhenDepartmentExists() {
        when(departmentDao.findByDeptNameAndIsDeletedFalse("IT")).thenReturn(Optional.of(department));
        assertThrows(ResourceAlreadyExistsException.class, () -> departmentService.createDepartment(dto));
    }

    @Test
    void createDepartmentShouldThrowWhenCityNotFound() {
        when(departmentDao.findByDeptNameAndIsDeletedFalse("IT")).thenReturn(Optional.empty());
        when(cityDao.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.createDepartment(dto));
    }

    @Test
    void updateDepartmentShouldUpdateWhenValid() {
        when(departmentDao.findById(1L)).thenReturn(Optional.of(department));
        when(cityDao.findById(1L)).thenReturn(Optional.of(city));
        when(departmentDao.save(any())).thenReturn(department);

        Department updated = departmentService.updateDepartment(1L, dto);
        assertEquals("IT", updated.getDeptName());
    }

    @Test
    void updateDepartmentShouldThrowWhenIdNotFound() {
        when(departmentDao.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.updateDepartment(2L, dto));
    }

    @Test
    void updateDepartmentShouldThrowWhenDuplicateName() {
        Department anotherDept = new Department();
        anotherDept.setDeptId(2L);
        anotherDept.setDeptName("HR");

        dto.setDeptName("HR");

        when(departmentDao.findById(1L)).thenReturn(Optional.of(department));
        when(departmentDao.findByDeptNameAndIsDeletedFalse("HR")).thenReturn(Optional.of(anotherDept));

        assertThrows(ResourceAlreadyExistsException.class, () -> departmentService.updateDepartment(1L, dto));
    }

    @Test
    void getUsersOfADepartmentByIdShouldReturnUsers() {
        User user = new User();
        user.setUserId(1L);
        department.setUsers(List.of(user));

        when(departmentDao.findById(1L)).thenReturn(Optional.of(department));

        List<User> users = departmentService.getUsersOfADepartmentById(1L);
        assertEquals(1, users.size());
    }

    @Test
    void getUsersOfADepartmentByIdShouldThrowWhenNotFound() {
        when(departmentDao.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.getUsersOfADepartmentById(1L));
    }

    @Test
    void deleteDepartmentByIdShouldSetIsDeletedTrue() {
        when(departmentDao.findById(1L)).thenReturn(Optional.of(department));
        departmentService.deleteDepartmentById(1L);
        assertTrue(department.getIsDeleted());
        verify(departmentDao).save(department);
    }

    @Test
    void deleteDepartmentByIdShouldThrowWhenNotFound() {
        when(departmentDao.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.deleteDepartmentById(2L));
    }
}