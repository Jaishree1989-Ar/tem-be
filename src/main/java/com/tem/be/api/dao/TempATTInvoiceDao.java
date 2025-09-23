package com.tem.be.api.dao;

import com.tem.be.api.model.TempATTInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TempATTInvoiceDao extends JpaRepository<TempATTInvoice, Long> {
    List<TempATTInvoice> findByInvoiceHistory_BatchId(String batchId);

    void deleteAllByInvoiceHistory_BatchId(String batchId);
}
