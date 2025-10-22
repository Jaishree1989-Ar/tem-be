package com.tem.be.api.model;


import javax.persistence.*;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

/**
 * Represents the mapping between a Foundation Account Number (FAN),
 * a specific Department Account Number, and a Department code.
 * This entity also tracks creation and upload metadata.
 */
@Data
@Entity
@Table(name = "account_department_mapping")
@SQLDelete(sql = "UPDATE account_department_mapping SET is_deleted = true WHERE mapping_id = ?")
public class AccountDepartmentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long id;

    @Column(name = "foundation_account_number")
    private String foundationAccountNumber;

    @Column(name = "department_account_number", nullable = false)
    private String departmentAccountNumber;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "carrier", nullable = false)
    private String carrier;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private String fileSize;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
