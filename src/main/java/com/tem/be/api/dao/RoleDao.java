package com.tem.be.api.dao;

import com.tem.be.api.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and managing User entities.
 */
@Repository
public interface RoleDao extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleNameAndIsDeletedFalse(String roleName);
    List<Role> findByIsDeletedFalse();
    List<Role> findByIsDeletedFalseOrderByUpdatedAtDesc();
}
