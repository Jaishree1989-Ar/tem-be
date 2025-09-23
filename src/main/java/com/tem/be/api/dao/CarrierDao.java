package com.tem.be.api.dao;

import com.tem.be.api.model.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and managing carrier entities.
 */
@Repository
public interface CarrierDao extends JpaRepository<Carrier, Long> {
    Optional<Carrier> findByCarrierNameAndIsDeletedFalse(String carrierName);
    List<Carrier> findByIsDeletedFalse();
}
