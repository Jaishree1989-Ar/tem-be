package com.tem.be.api.dao;

import com.tem.be.api.model.RoleModuleAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleModuleAccessDao extends JpaRepository<RoleModuleAccess, Long> {
}
