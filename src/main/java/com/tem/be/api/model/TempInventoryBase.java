package com.tem.be.api.model;

import com.tem.be.api.enums.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@MappedSuperclass
public abstract class TempInventoryBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_history_id", nullable = false)
    private InventoryHistory inventoryHistory;

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
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private InvoiceStatus status;

    @Column(name = "department")
    private String department;
}
