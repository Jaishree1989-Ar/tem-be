package com.tem.be.api.model;

import javax.persistence.*;

import com.tem.be.api.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Getter
@Setter
@MappedSuperclass
public abstract class TempInvoiceBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_history_id", nullable = false)
    private InvoiceHistory invoiceHistory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "source_filename")
    private String sourceFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvoiceStatus status;

    @Column(name = "invoice_number")
    private String invoiceNumber;
}
