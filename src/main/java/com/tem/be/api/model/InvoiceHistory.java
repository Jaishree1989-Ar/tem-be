package com.tem.be.api.model;

import com.tem.be.api.enums.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoice_history")
public class InvoiceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "batch_id", unique = true, nullable = false)
    private String batchId;

    @OneToMany(mappedBy = "invoiceHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TempFirstNetInvoice> tempInvoices = new ArrayList<>();

    @OneToMany(mappedBy = "invoiceHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TempATTInvoice> tempATTInvoices = new ArrayList<>();

    @OneToMany(mappedBy = "invoiceHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TempVerizonWirelessInvoice> tempVerizonInvoices = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvoiceStatus status;

    @CreationTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "name")
    private String name;

    @Column(name = "carrier")
    private String carrier;

    @CreationTimestamp
    @Column(name = "date_uploaded", updatable = false)
    private Timestamp dateUploaded;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "file_size")
    private String fileSize;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "is_deleted")
    private Boolean isDeleted = Boolean.FALSE;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    @Column(name = "rejection_reason", length = 1024)
    private String rejectionReason;
}
