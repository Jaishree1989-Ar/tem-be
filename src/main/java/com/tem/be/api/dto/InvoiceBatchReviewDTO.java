package com.tem.be.api.dto;

import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempInvoiceBase;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InvoiceBatchReviewDTO {
    private InvoiceHistory batchDetails;
    private List<? extends TempInvoiceBase> invoiceRecords;
}
