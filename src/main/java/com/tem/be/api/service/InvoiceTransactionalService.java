package com.tem.be.api.service;

import com.tem.be.api.dao.TempFirstNetInvoiceDao;
import com.tem.be.api.model.TempFirstNetInvoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvoiceTransactionalService {

    private TempFirstNetInvoiceDao tempFirstNetInvoiceDao;


    @Autowired
    public void setTempFirstNetInvoiceDao(TempFirstNetInvoiceDao tempFirstNetInvoiceDao) {
        this.tempFirstNetInvoiceDao = tempFirstNetInvoiceDao;
    }

    /**
     * Saves the list of temporary invoices in its own transaction.
     * This ensures atomicity for the batch data insertion.
     */
    @Transactional
    public void saveTempInvoices(List<TempFirstNetInvoice> tempInvoices) {
        tempFirstNetInvoiceDao.saveAll(tempInvoices);
    }
}
