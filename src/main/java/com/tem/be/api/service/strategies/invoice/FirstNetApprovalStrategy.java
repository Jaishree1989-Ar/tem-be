package com.tem.be.api.service.strategies.invoice;

import com.tem.be.api.dao.FirstNetInvoiceDao;
import com.tem.be.api.dao.TempFirstNetInvoiceDao;
import com.tem.be.api.model.FirstNetInvoice;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempFirstNetInvoice;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class FirstNetApprovalStrategy implements ApprovalStrategy {

    private final TempFirstNetInvoiceDao tempRepo;
    private final FirstNetInvoiceDao finalRepo;

    @Autowired
    public FirstNetApprovalStrategy(TempFirstNetInvoiceDao tempRepo, FirstNetInvoiceDao finalRepo) {
        this.tempRepo = tempRepo;
        this.finalRepo = finalRepo;
    }

    @Override
    public String getProviderName() {
        return "FirstNet";
    }

    @Override
    public List<TempFirstNetInvoice> getTemporaryInvoicesForReview(String batchId) {
        return tempRepo.findByInvoiceHistory_BatchId(batchId);
    }

    @Override
    @Transactional
    public void approve(InvoiceHistory history) {
        List<TempFirstNetInvoice> tempInvoices = tempRepo.findByInvoiceHistory_BatchId(history.getBatchId());

        List<FirstNetInvoice> finalInvoices = tempInvoices.stream()
                .map(temp -> convertToFinalInvoice(temp, history))
                .toList();

        finalRepo.saveAll(finalInvoices);
        tempRepo.deleteAll(tempInvoices); // Clean up temp data
    }

    @Override
    @Transactional
    public void reject(String batchId) {
        tempRepo.deleteAllByInvoiceHistory_BatchId(batchId);
    }

    @Override
    public List<FirstNetInvoice> getFinalInvoices(String batchId) {
        return finalRepo.findByInvoiceHistory_BatchId(batchId);
    }

    private FirstNetInvoice convertToFinalInvoice(TempFirstNetInvoice tempInvoice, InvoiceHistory history) {
        FirstNetInvoice finalInvoice = new FirstNetInvoice();
        // Ignore properties that don't exist or shouldn't be copied
        BeanUtils.copyProperties(tempInvoice, finalInvoice, "invoiceId", "invoiceHistory", "sourceFilename", "status", "createdAt", "updatedAt");
        finalInvoice.setInvoiceHistory(history);
        return finalInvoice;
    }
}
