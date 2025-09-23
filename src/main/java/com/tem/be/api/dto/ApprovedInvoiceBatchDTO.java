package com.tem.be.api.dto;

import com.tem.be.api.model.InvoiceHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovedInvoiceBatchDTO {
    private InvoiceHistory batchDetails;
    private List<?> invoiceRecords;
}
