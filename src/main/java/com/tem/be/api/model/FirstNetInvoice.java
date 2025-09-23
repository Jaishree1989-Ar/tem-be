package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Represents a firstnet invoice entity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "firstnet_invoices")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FirstNetInvoice implements Invoiceable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_history_id")
    @JsonIgnore
    private InvoiceHistory invoiceHistory;

    @CreationTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "foundation_account")
    private String foundationAccount;

    @Column(name = "foundation_account_name")
    private String foundationAccountName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_and_descriptions")
    private String accountAndDescriptions;

    @Column(name = "billing_account_name")
    private String billingAccountName;

    @Column(name = "wireless_number_and_descriptions")
    private String wirelessNumberAndDescriptions;

    @Column(name = "wireless_number")
    private String wirelessNumber;

    @Column(name = "department")
    private String department;

    @Column(name = "division")
    private String division;

    @Column(name = "device_class")
    private String deviceClass;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "vis_code")
    private String visCode;

    @Column(name = "udl_4")
    private String udl4;

    @Column(name = "market_cycle_end_date")
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date marketCycleEndDate;

    @Column(name = "invoice_date")
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date invoiceDate;

    @Column(name = "rate_code")
    private String rateCode;

    @Column(name = "rate_plan_name")
    private String ratePlanName;

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "total_current_charges")
    private String totalCurrentCharges;

    @Column(name = "total_monthly_charges")
    private String totalMonthlyCharges;

    @Column(name = "total_activity_since_last_bill")
    private String totalActivitySinceLastBill;

    @Column(name = "total_taxes")
    private String totalTaxes;

    @Column(name = "total_company_fees_and_surcharges")
    private String totalCompanyFeesAndSurcharges;

    @Column(name = "total_kb_usage")
    private String totalKbUsage;

    @Column(name = "total_minutes_usage")
    private String totalMinutesUsage;

    @Column(name = "total_messages")
    private String totalMessages;

    @Column(name = "total_fan_level_charges")
    private String totalFanLevelCharges;

    @Column(name = "total_adjustments")
    private String totalAdjustments;

    @Column(name = "total_reoccurring_charges", precision = 19, scale = 4)
    private BigDecimal totalReoccurringCharges;
}
