package com.tem.be.api.dao;

import com.tem.be.api.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and managing department entities.
 */
@Repository
public interface DepartmentDao extends JpaRepository<Department, Long> {
    Optional<Department> findByDeptNameAndIsDeletedFalse(String departmentName);
    List<Department> findByIsDeletedFalse();
}
