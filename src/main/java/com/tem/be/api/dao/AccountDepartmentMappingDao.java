package com.tem.be.api.dao;

import com.tem.be.api.model.AccountDepartmentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountDepartmentMappingDao extends JpaRepository<AccountDepartmentMapping, Long> {

    /**
     * Finds a single, active mapping by its ID.
     *
     * @param id The ID of the mapping.
     * @return An Optional containing the active mapping if found.
     */
    Optional<AccountDepartmentMapping> findByIdAndIsDeletedFalse(Long id);

    /**
     * Finds all active mappings, ordered by creation date descending.
     *
     * @return A list of all active mappings.
     */
    List<AccountDepartmentMapping> findByCarrierAndIsDeletedFalseOrderByCreatedAtDesc(String carrier);

    /**
     * Checks if an active mapping with the given department account number exists.
     *
     * @param departmentAccountNumber The account number to check.
     * @return true if an active mapping exists, false otherwise.
     */
    boolean existsByDepartmentAccountNumberAndIsDeletedFalse(String departmentAccountNumber);

    List<AccountDepartmentMapping> findAllByCarrier(String carrier);
}
