package com.tem.be.api.service.strategies.invoice;

import com.tem.be.api.dao.ATTInvoiceDao;
import com.tem.be.api.dao.TempATTInvoiceDao;
import com.tem.be.api.model.ATTInvoice;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempATTInvoice;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ATTApprovalStrategy implements ApprovalStrategy {

    private final TempATTInvoiceDao tempRepo;
    private final ATTInvoiceDao finalRepo;

    @Autowired
    public ATTApprovalStrategy(TempATTInvoiceDao tempRepo, ATTInvoiceDao finalRepo) {
        this.tempRepo = tempRepo;
        this.finalRepo = finalRepo;
    }

    @Override
    public String getProviderName() {
        return "AT&T Mobility";
    }

    @Override
    public List<TempATTInvoice> getTemporaryInvoicesForReview(String batchId) {
        return tempRepo.findByInvoiceHistory_BatchId(batchId);
    }

    @Override
    @Transactional
    public void approve(InvoiceHistory history) {
        List<TempATTInvoice> tempInvoices = tempRepo.findByInvoiceHistory_BatchId(history.getBatchId());

        List<ATTInvoice> finalInvoices = tempInvoices.stream()
                .map(temp -> convertToFinalInvoice(temp, history))
                .toList();

        finalRepo.saveAll(finalInvoices);
        tempRepo.deleteAll(tempInvoices);
    }

    @Override
    @Transactional
    public void reject(String batchId) {
        tempRepo.deleteAllByInvoiceHistory_BatchId(batchId);
    }

    @Override
    public List<ATTInvoice> getFinalInvoices(String batchId) {
        return finalRepo.findByInvoiceHistory_BatchId(batchId);
    }

    private ATTInvoice convertToFinalInvoice(TempATTInvoice tempInvoice, InvoiceHistory history) {
        ATTInvoice finalInvoice = new ATTInvoice();
        BeanUtils.copyProperties(tempInvoice, finalInvoice, "invoiceId", "invoiceHistory", "sourceFilename", "status", "createdAt", "updatedAt");
        finalInvoice.setInvoiceHistory(history);
        return finalInvoice;
    }
}
