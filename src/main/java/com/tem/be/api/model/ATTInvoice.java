package com.tem.be.api.model;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "att_invoices")
public class ATTInvoice implements Invoiceable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_history_id")
    @JsonIgnore
    private InvoiceHistory invoiceHistory;

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

    @Column(name = "department") // From sample data
    private String department;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "vis_code") // From sample data
    private String visCode;

    @Column(name = "asset_tag") // From sample data
    private String assetTag;

    @Temporal(TemporalType.DATE)
    @Column(name = "market_cycle_end_date")
    private Date marketCycleEndDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "invoice_date")
    private Date invoiceDate;

    @Column(name = "invoice_number")
    private String invoiceNumber;

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

    // Optional: Add calculated field if needed
    @Column(name = "total_reoccurring_charges", precision = 19, scale = 4)
    private BigDecimal totalReoccurringCharges;
}
