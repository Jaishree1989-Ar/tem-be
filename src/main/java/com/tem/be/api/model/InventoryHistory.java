package com.tem.be.api.model;

import com.tem.be.api.enums.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "inventory_history")
public class InventoryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_history_id")
    private Long inventoryHistoryId;

    @Column(name = "batch_id", unique = true, nullable = false)
    private String batchId;

    @OneToMany(mappedBy = "inventoryHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TempFirstNetInventory> tempInventories = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvoiceStatus status; // Reusing the same status enum

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
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

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    @Column(name = "rejection_reason", length = 1024)
    private String rejectionReason;
}
