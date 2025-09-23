package com.tem.be.api.dao;

import com.tem.be.api.model.VerizonInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for accessing and managing verizon invoice entities.
 */
@Repository
public interface VerizonInvoiceDao extends JpaRepository<VerizonInvoice, Long> {
}
