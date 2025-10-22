package com.tem.be.api.service.strategies.invoice;

import com.tem.be.api.dao.TempVerizonWirelessInvoiceDao;
import com.tem.be.api.dao.VerizonWirelessInvoiceDao;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempVerizonWirelessInvoice;
import com.tem.be.api.model.VerizonWirelessInvoice;
import com.tem.be.api.utils.CarrierConstants;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@AllArgsConstructor
public class VerizonWirelessApprovalStrategy implements ApprovalStrategy {

    private final TempVerizonWirelessInvoiceDao tempRepo;
    private final VerizonWirelessInvoiceDao finalRepo;

    @Override
    public String getProviderName() {
        return CarrierConstants.VERIZON_WIRELESS;
    }

    @Override
    public List<TempVerizonWirelessInvoice> getTemporaryInvoicesForReview(String batchId) {
        return tempRepo.findByInvoiceHistory_BatchId(batchId);
    }

    @Override
    @Transactional
    public void approve(InvoiceHistory history) {
        List<TempVerizonWirelessInvoice> tempInvoices = tempRepo.findByInvoiceHistory_BatchId(history.getBatchId());

        List<VerizonWirelessInvoice> finalInvoices = tempInvoices.stream()
                .map(temp -> convertToFinalInvoice(temp, history))
                .toList();

        finalRepo.saveAll(finalInvoices);
        tempRepo.deleteAllInBatch(tempInvoices); // Use batch delete for efficiency
    }

    @Override
    @Transactional
    public void reject(String batchId) {
        tempRepo.deleteAllByInvoiceHistory_BatchId(batchId);
    }

    @Override
    public List<VerizonWirelessInvoice> getFinalInvoices(String batchId) {
        return finalRepo.findByInvoiceHistory_BatchId(batchId);
    }

    private VerizonWirelessInvoice convertToFinalInvoice(TempVerizonWirelessInvoice temp, InvoiceHistory history) {
        VerizonWirelessInvoice finalInvoice = new VerizonWirelessInvoice();

        // This will now copy all 40 matching fields automatically.
        BeanUtils.copyProperties(temp, finalInvoice,
                "invoiceId", "invoiceHistory", "sourceFilename", "status", "createdAt", "updatedAt");

        finalInvoice.setInvoiceHistory(history);
        return finalInvoice;
    }
}
