package com.tem.be.api.dao;

import com.tem.be.api.model.TempVerizonWirelessInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TempVerizonWirelessInvoiceDao extends JpaRepository<TempVerizonWirelessInvoice, Long> {
    List<TempVerizonWirelessInvoice> findByInvoiceHistory_BatchId(String batchId);

    void deleteAllByInvoiceHistory_BatchId(String batchId);
}
