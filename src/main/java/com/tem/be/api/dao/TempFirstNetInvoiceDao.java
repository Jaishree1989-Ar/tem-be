package com.tem.be.api.dao;

import com.tem.be.api.model.TempFirstNetInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TempFirstNetInvoiceDao extends JpaRepository<TempFirstNetInvoice, Long> {

    List<TempFirstNetInvoice> findByInvoiceHistory_BatchId(String batchId);

    void deleteAllByInvoiceHistory_BatchId(String batchId);
}
